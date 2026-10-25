package com.paybridge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.AiExplanationRequest;
import com.paybridge.dto.AiExplanationResponse;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.exception.InvalidEvaluationRequestException;
import com.paybridge.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service that generates concise, human-readable explanations of existing compatibility evaluations.
 *
 * CRITICAL TRUST BOUNDARIES:
 * - The AI service explains existing decisions only.
 * - The AI explanation MUST NOT change the compatibility status, invent provider support, or authorize payments.
 * - If the external AI service is unavailable or unconfigured, the service falls back gracefully
 *   to a deterministic explanation matching the recorded evaluation.
 */
@Service
public class AiExplanationService {

    private static final Logger log = LoggerFactory.getLogger(AiExplanationService.class);

    private final CompatibilityEngineService compatibilityEngineService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    public AiExplanationService(CompatibilityEngineService compatibilityEngineService, ObjectMapper objectMapper) {
        this.compatibilityEngineService = compatibilityEngineService;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public AiExplanationResponse explain(AiExplanationRequest request) {
        if (request == null || request.evaluationId() == null || request.evaluationId().isBlank()) {
            throw new InvalidEvaluationRequestException("Request body must contain non-blank 'evaluationId'.");
        }

        String evaluationId = request.evaluationId().trim();
        CompatibilityEvaluationResponse evaluation = compatibilityEngineService.findEvaluationById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation with ID '" + evaluationId + "' was not found."));

        // Generate explanation: try live Gemini API if configured, otherwise use deterministic fallback
        String explanation = null;
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                explanation = callGeminiApi(evaluation);
            } catch (Exception e) {
                log.warn("Gemini API call failed for evaluation '{}'; activating deterministic fallback: {}", evaluationId, e.getMessage());
            }
        }

        if (explanation == null || explanation.isBlank()) {
            explanation = generateFallbackExplanation(evaluation);
        }

        List<String> limitations = evaluation.limitations() != null
                ? new ArrayList<>(evaluation.limitations())
                : new ArrayList<>();

        if (limitations.isEmpty()) {
            limitations.add("Compatibility has not been confirmed.");
        }

        return new AiExplanationResponse(evaluationId, explanation, Collections.unmodifiableList(limitations));
    }

    private String generateFallbackExplanation(CompatibilityEvaluationResponse evaluation) {
        return switch (evaluation.status()) {
            case COMPATIBLE ->
                    "Bilateral payment route linkage confirms compatibility between the visitor's provider and domestic UPI merchants. "
                            + evaluation.reason();
            case CONDITIONAL ->
                    "Payment route compatibility is conditional. "
                            + evaluation.reason()
                            + " Verification requires the merchant terminal and visitor provider to satisfy all stated prerequisites.";
            case UNVERIFIED ->
                    "Provider support has not been verified. Further evidence is required. "
                            + evaluation.reason();
        };
    }

    private String callGeminiApi(CompatibilityEvaluationResponse evaluation) {
        String prompt = buildPrompt(evaluation);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> requestPayload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String rawResponse = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String.class);

        return parseGeminiResponseText(rawResponse);
    }

    private String buildPrompt(CompatibilityEvaluationResponse evaluation) {
        return """
                You are PayBridge AI's technical explanation assistant. Explain the following payment route evaluation to an international traveler.
                Strict Rules:
                1. You MUST NOT change the compatibility status (%s) or contradict the authoritative reason.
                2. You MUST NOT claim that PayPal or any other provider can pay the UPI merchant unless proven by the evidence.
                3. You MUST NOT claim that scanning a QR verifies merchant identity.
                4. You MUST NOT authorize or guarantee payment execution.
                5. Keep the explanation under 3 sentences.

                Evaluation Details:
                - Status: %s
                - Reason: %s
                - Evidence: %s
                """.formatted(
                evaluation.status(),
                evaluation.status(),
                evaluation.reason(),
                evaluation.evidence()
        );
    }

    private String parseGeminiResponseText(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!textNode.isMissingNode()) {
                return textNode.asText().trim();
            }
        } catch (Exception e) {
            log.warn("Error parsing Gemini API response JSON: {}", e.getMessage());
        }
        return null;
    }
}
