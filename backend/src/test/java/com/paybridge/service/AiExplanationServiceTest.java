package com.paybridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.AiExplanationRequest;
import com.paybridge.dto.AiExplanationResponse;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.dto.CompatibilityStatus;
import com.paybridge.dto.EvidenceItem;
import com.paybridge.exception.InvalidEvaluationRequestException;
import com.paybridge.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiExplanationServiceTest {

    private AiExplanationService aiExplanationService;
    private CompatibilityEngineService compatibilityEngineService;

    @BeforeEach
    void setUp() {
        QrParserService qrParserService = new QrParserService();
        EvidenceStore evidenceStore = new EvidenceStore();
        compatibilityEngineService = new CompatibilityEngineService(qrParserService, evidenceStore);
        aiExplanationService = new AiExplanationService(compatibilityEngineService, new ObjectMapper());
    }

    @Test
    @DisplayName("AI-01: Explain a saved COMPATIBLE evaluation matches recorded decision and evidence")
    void testExplain_CompatibleEvaluation() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-001",
                CompatibilityStatus.COMPATIBLE,
                "Documented bilateral linkage confirms routing support for upi-global from SG to INR UPI merchants.",
                List.of(new EvidenceItem("NPCI International Agreement", "Direct linkage active.", "SUPPORTED", true)),
                List.of("Parsing does not verify merchant identity."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-001"));

        assertThat(response.evaluationId()).isEqualTo("EVAL-001");
        assertThat(response.explanation()).contains("Bilateral payment route linkage confirms compatibility");
        assertThat(response.explanation()).contains("upi-global");
        assertThat(response.limitations()).contains("Parsing does not verify merchant identity.");
    }

    @Test
    @DisplayName("AI-02: Explain a saved CONDITIONAL evaluation describes conditions and limitations")
    void testExplain_ConditionalEvaluation() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-002",
                CompatibilityStatus.CONDITIONAL,
                "Merchant terminal acquiring bank must have international UPI acceptance enabled.",
                List.of(new EvidenceItem("Circular 2024", "Conditional merchant enablement.", "CONDITIONAL", true)),
                List.of("Merchant terminal must be enrolled in international acceptance."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-002"));

        assertThat(response.evaluationId()).isEqualTo("EVAL-002");
        assertThat(response.explanation()).contains("Payment route compatibility is conditional");
        assertThat(response.explanation()).contains("acquiring bank must have international UPI acceptance enabled");
        assertThat(response.limitations()).contains("Merchant terminal must be enrolled in international acceptance.");
    }

    @Test
    @DisplayName("AI-03: Explain an UNVERIFIED evaluation communicates uncertainty clearly")
    void testExplain_UnverifiedEvaluation() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-003",
                CompatibilityStatus.UNVERIFIED,
                "Provider support has not been verified. No authoritative documentation establishes payment route compatibility.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-003"));

        assertThat(response.evaluationId()).isEqualTo("EVAL-003");
        assertThat(response.explanation()).contains("Provider support has not been verified. Further evidence is required.");
        assertThat(response.limitations()).contains("Compatibility has not been confirmed.");
    }

    @Test
    @DisplayName("AI-04: Graceful fallback when external Gemini API is unconfigured/unavailable")
    void testExplain_FallbackWhenGeminiUnavailable() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-004",
                CompatibilityStatus.UNVERIFIED,
                "No authoritative documentation available.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        // Without GEMINI_API_KEY set, fallback executes safely
        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-004"));

        assertThat(response).isNotNull();
        assertThat(response.explanation()).isNotEmpty();
    }

    @Test
    @DisplayName("AI-05: Non-existent evaluation ID throws ResourceNotFoundException")
    void testExplain_UnknownEvaluationId_ThrowsNotFound() {
        assertThatThrownBy(() -> aiExplanationService.explain(new AiExplanationRequest("EVAL-NONEXISTENT")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Evaluation with ID 'EVAL-NONEXISTENT' was not found");
    }

    @Test
    @DisplayName("Missing or blank evaluation ID throws InvalidEvaluationRequestException")
    void testExplain_BlankEvaluationId_ThrowsException() {
        assertThatThrownBy(() -> aiExplanationService.explain(new AiExplanationRequest("   ")))
                .isInstanceOf(InvalidEvaluationRequestException.class)
                .hasMessageContaining("Request body must contain non-blank 'evaluationId'");

        assertThatThrownBy(() -> aiExplanationService.explain(null))
                .isInstanceOf(InvalidEvaluationRequestException.class)
                .hasMessageContaining("Request body must contain non-blank 'evaluationId'");
    }

    @Test
    @DisplayName("AI-06: Untrusted evaluation data preserves recorded limitations")
    void testExplain_PreservesLimitations() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-006",
                CompatibilityStatus.UNVERIFIED,
                "Unknown provider.",
                List.of(),
                List.of("Parsing does not verify merchant legal identity.", "Payment execution is not authorized."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-006"));

        assertThat(response.limitations())
                .contains("Parsing does not verify merchant legal identity.")
                .contains("Payment execution is not authorized.");
    }
}
