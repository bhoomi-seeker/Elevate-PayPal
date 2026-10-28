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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiExplanationServiceTest {

    private AiExplanationService aiExplanationService;
    private CompatibilityEngineService compatibilityEngineService;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl = "https://generativelanguage.googleapis.com";
    private final String model = "gemini-3.5-flash-lite";
    private final String expectedEndpoint = baseUrl + "/v1beta/models/" + model + ":generateContent";

    @BeforeEach
    void setUp() {
        QrParserService qrParserService = new QrParserService();
        EvidenceStore evidenceStore = new EvidenceStore();
        compatibilityEngineService = new CompatibilityEngineService(qrParserService, evidenceStore);

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        aiExplanationService = new AiExplanationService(compatibilityEngineService, objectMapper, restClientBuilder.build());
        aiExplanationService.configure(baseUrl, model, "test-api-key");
    }

    @Test
    @DisplayName("AI-01: Explain a saved COMPATIBLE evaluation matches recorded decision and evidence")
    void testExplain_CompatibleEvaluation() {
        aiExplanationService.configure(baseUrl, model, ""); // Test fallback when key empty

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
        assertThat(response.explanation()).contains("The recorded evaluation indicates that the payment route meets the documented compatibility criteria");
        assertThat(response.explanation()).contains("upi-global");
        assertThat(response.limitations()).contains("Parsing does not verify merchant identity.");
    }

    @Test
    @DisplayName("AI-02: Explain a saved CONDITIONAL evaluation describes conditions and limitations")
    void testExplain_ConditionalEvaluation() {
        aiExplanationService.configure(baseUrl, model, "");

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
        aiExplanationService.configure(baseUrl, model, "");

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
        aiExplanationService.configure(baseUrl, model, "");

        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-004",
                CompatibilityStatus.UNVERIFIED,
                "No authoritative documentation available.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

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
        aiExplanationService.configure(baseUrl, model, "");

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

    @Test
    @DisplayName("Gemini Success: Parses live 200 OK response and extracts explanation text")
    void testExplain_GeminiSuccess_ParsesText() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-01",
                CompatibilityStatus.COMPATIBLE,
                "Bilateral agreement confirmed.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Your payment method is compatible for this UPI QR transaction."
                                  }
                                ]
                              },
                              "finishReason": "STOP"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-GEMINI-01"));

        assertThat(response.explanation()).isEqualTo("Your payment method is compatible for this UPI QR transaction.");
        assertThat(response.limitations()).contains("Compatibility has not been confirmed.");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini Thinking Model: Skips internal thought parts and extracts non-thought text")
    void testExplain_GeminiThinkingModel_ExtractsAnswer() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-02",
                CompatibilityStatus.CONDITIONAL,
                "Terminal prerequisite required.",
                List.of(),
                List.of("Prerequisite required."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "thought": true,
                                    "text": "The user needs an explanation. I will note the condition."
                                  },
                                  {
                                    "thought": false,
                                    "text": "Payment is conditional upon terminal enablement by the merchant acquiring bank."
                                  }
                                ]
                              },
                              "finishReason": "STOP"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-GEMINI-02"));

        assertThat(response.explanation()).isEqualTo("Payment is conditional upon terminal enablement by the merchant acquiring bank.");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini Thinking Model: Falls back when response contains only internal thought parts")
    void testExplain_GeminiThinkingModel_OnlyThoughtPartsFallsBack() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-12",
                CompatibilityStatus.UNVERIFIED,
                "Provider support has not been verified.",
                List.of(),
                List.of("Further evidence is required."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "thought": true,
                                    "text": "INTERNAL REASONING MUST NOT BE RETURNED"
                                  },
                                  {
                                    "thought": true,
                                    "text": "MORE INTERNAL REASONING"
                                  }
                                ]
                              },
                              "finishReason": "STOP"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(
                new AiExplanationRequest("EVAL-GEMINI-12"));

        assertThat(response.explanation())
                .contains("Provider support has not been verified.");
        assertThat(response.explanation())
                .doesNotContain("INTERNAL REASONING MUST NOT BE RETURNED");
        assertThat(response.explanation())
                .doesNotContain("MORE INTERNAL REASONING");

        mockServer.verify();
    }
    @Test
    @DisplayName("Gemini 503 Transient: Retries on 503 and returns explanation on successful retry")
    void testExplain_Gemini503_RetriesAndSucceeds() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-03",
                CompatibilityStatus.COMPATIBLE,
                "Route active.",
                List.of(),
                List.of("Baseline limitation."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        // Attempt 1: 503
        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("""
                        {
                          "error": {
                            "code": 503,
                            "message": "The model is overloaded. Please try again later.",
                            "status": "UNAVAILABLE"
                          }
                        }
                        """));

        // Attempt 2: 200 OK
        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Routing is supported under current bilateral arrangements."
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-GEMINI-03"));

        assertThat(response.explanation()).isEqualTo("Routing is supported under current bilateral arrangements.");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini 503 Exhausted: Falls back gracefully to deterministic explanation after retries")
    void testExplain_Gemini503_ExhaustedFallsBack() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-04",
                CompatibilityStatus.UNVERIFIED,
                "No provider support found.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        // All attempts return 503 (initial attempt + 2 retries = 3 calls)
        mockServer.expect(ExpectedCount.times(3), requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("""
                        {
                          "error": {
                            "code": 503,
                            "message": "Service unavailable due to high demand.",
                            "status": "UNAVAILABLE"
                          }
                        }
                        """));

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-GEMINI-04"));

        // Verifies fallback was activated
        assertThat(response.explanation()).contains("Provider support has not been verified. Further evidence is required.");
        assertThat(response.limitations()).contains("Compatibility has not been confirmed.");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini 404 Model Not Found: Fails fast without retries and activates fallback")
    void testExplain_Gemini404_FailsFastToFallback() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-05",
                CompatibilityStatus.UNVERIFIED,
                "Evaluation reason.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        // 404 should be called exactly once (no retries)
        mockServer.expect(ExpectedCount.once(), requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("""
                        {
                          "error": {
                            "code": 404,
                            "message": "models/gemini-old is not found for API version v1beta.",
                            "status": "NOT_FOUND"
                          }
                        }
                        """));

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-GEMINI-05"));

        assertThat(response.explanation()).contains("Provider support has not been verified. Further evidence is required.");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini Malformed JSON: Safely activates fallback without unhandled exceptions")
    void testExplain_GeminiMalformedJson_FallsBack() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-06",
                CompatibilityStatus.COMPATIBLE,
                "Route active.",
                List.of(),
                List.of("Limitation."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("Not Valid JSON", MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-GEMINI-06"));

        assertThat(response.explanation()).contains("The recorded evaluation indicates that the payment route meets the documented compatibility criteria");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini Empty Candidates: Activates fallback when candidates array is empty")
    void testExplain_GeminiEmptyCandidates_FallsBack() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-07",
                CompatibilityStatus.COMPATIBLE,
                "Route active.",
                List.of(),
                List.of("Limitation."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-GEMINI-07"));

        assertThat(response.explanation()).contains("The recorded evaluation indicates that the payment route meets the documented compatibility criteria");
        mockServer.verify();
    }

    @Test
    @DisplayName("Rejects unsupported Gemini claims for UNVERIFIED evaluations")
    void testExplain_UnverifiedEvaluation_RejectsUnsupportedGeminiClaim() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-09",
                CompatibilityStatus.UNVERIFIED,
                "No authoritative documentation establishes payment route compatibility.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Your payment is guaranteed to work successfully."
                                  }
                                ]
                              },
                              "finishReason": "STOP"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(
                new AiExplanationRequest("EVAL-GEMINI-09"));

        assertThat(response.explanation())
                .contains("Provider support has not been verified. Further evidence is required.")
                .doesNotContain("guaranteed to work");
        mockServer.verify();
    }
    @Test
    @DisplayName("Rejects unsupported Gemini guarantees for CONDITIONAL evaluations")
    void testExplain_ConditionalEvaluation_RejectsUnsupportedGuarantee() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-10",
                CompatibilityStatus.CONDITIONAL,
                "Compatibility depends on merchant terminal and provider prerequisites.",
                List.of(),
                List.of("Merchant and provider prerequisites require verification."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Your payment is assured to succeed."
                                  }
                                ]
                              },
                              "finishReason": "STOP"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(
                new AiExplanationRequest("EVAL-GEMINI-10"));

        assertThat(response.explanation())
                .contains("Payment route compatibility is conditional.")
                .doesNotContain("assured to succeed");
        mockServer.verify();
    }
    @Test
    @DisplayName("Rejects unsupported Gemini guarantees for COMPATIBLE evaluations")
    void testExplain_CompatibleEvaluation_RejectsUnsupportedGuarantee() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-11",
                CompatibilityStatus.COMPATIBLE,
                "Route active.",
                List.of(),
                List.of("Limitation."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Your payment is guaranteed to succeed."
                                  }
                                ]
                              },
                              "finishReason": "STOP"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(
                new AiExplanationRequest("EVAL-GEMINI-11"));

        assertThat(response.explanation())
                .contains("The recorded evaluation indicates that the payment route meets the documented compatibility criteria")
                .doesNotContain("guaranteed to succeed");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini Safety Block: Activates fallback when candidate is blocked by policy")
    void testExplain_GeminiSafetyBlock_FallsBack() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-08",
                CompatibilityStatus.UNVERIFIED,
                "Unknown route.",
                List.of(),
                List.of("Limitation."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "finishReason": "SAFETY"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(new AiExplanationRequest("EVAL-GEMINI-08"));

        assertThat(response.explanation()).contains("Provider support has not been verified. Further evidence is required.");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini Max Tokens: Activates fallback when response is truncated")
    void testExplain_GeminiMaxTokens_FallsBack() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-13",
                CompatibilityStatus.UNVERIFIED,
                "Unknown route.",
                List.of(),
                List.of("Limitation."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Provider support appears to be verified, but the explanation is incomplete"
                                  }
                                ]
                              },
                              "finishReason": "MAX_TOKENS"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(
                new AiExplanationRequest("EVAL-GEMINI-13"));

        assertThat(response.explanation())
                .contains("Provider support has not been verified. Further evidence is required.")
                .doesNotContain("the explanation is incomplete");
        mockServer.verify();
    }

    @Test
    @DisplayName("Rejects false provider verification claims")
    void testExplain_UnverifiedEvaluation_RejectsFalseProviderVerification() {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-GEMINI-12",
                CompatibilityStatus.UNVERIFIED,
                "No authoritative documentation establishes payment route compatibility.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Provider support is verified and compatibility is confirmed."
                                  }
                                ]
                              },
                              "finishReason": "STOP"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse response = aiExplanationService.explain(
                new AiExplanationRequest("EVAL-GEMINI-12"));

        assertThat(response.explanation())
                .contains("Provider support has not been verified. Further evidence is required.")
                .doesNotContain("Provider support is verified")
                .doesNotContain("compatibility is confirmed");
        mockServer.verify();
    }
}
