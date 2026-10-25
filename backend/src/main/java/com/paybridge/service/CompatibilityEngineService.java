package com.paybridge.service;

import com.paybridge.dto.CompatibilityEvaluationRequest;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.dto.CompatibilityStatus;
import com.paybridge.dto.EvidenceItem;
import com.paybridge.dto.QrParseResponse;
import com.paybridge.exception.InvalidEvaluationRequestException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Deterministic payment compatibility evaluation engine.
 *
 * Evaluates candidate payment routes using explicit documented rules and evidence checks.
 *
 * IMPORTANT PRINCIPLES:
 * - A parsed QR payload does not establish merchant identity or legitimacy.
 * - Route compatibility claims must be supported by documented evidence; unknown support remains UNVERIFIED.
 * - PayPal Sandbox checkout is a separate payment-flow demonstration and does not transfer funds to the
 *   merchant encoded in a scanned UPI QR code.
 */
@Service
public class CompatibilityEngineService {

    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Za-z]{2}$");

    private final QrParserService qrParserService;
    private final EvidenceStore evidenceStore;

    // In-memory evaluation store to support future retrieval (e.g. GET /api/evaluations and /api/ai/explain)
    private final Map<String, CompatibilityEvaluationResponse> evaluationHistory = new ConcurrentHashMap<>();

    public CompatibilityEngineService(QrParserService qrParserService, EvidenceStore evidenceStore) {
        this.qrParserService = qrParserService;
        this.evidenceStore = evidenceStore;
    }

    public CompatibilityEvaluationResponse evaluate(CompatibilityEvaluationRequest request) {
        validateRequest(request);

        // 1. Verify that the QR payload is structurally valid UPI data.
        // Successful QR parsing confirms format validity, but never implies payment compatibility.
        QrParseResponse parsedQr = qrParserService.parse(request.qrText());

        String candidate = request.candidateProvider().trim().toLowerCase();
        String country = request.visitorCountry().trim().toUpperCase();
        String evaluationId = "EVAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant evaluatedAt = Instant.now();

        // 2. Deterministic evaluation rules
        CompatibilityStatus status;
        String reason;
        List<EvidenceItem> evidence = new ArrayList<>();
        List<String> limitations = new ArrayList<>();

        // Universal baseline limitations
        limitations.add("Parsing does not verify merchant legal identity, bank account legitimacy, or terminal security.");
        limitations.add("Compatibility evaluation reflects documented technical capabilities and does not guarantee bank authorization at transaction time.");

        // Rule for PayPal: PayPal Sandbox is a separate test workflow, not a direct UPI settlement rail
        if ("paypal".equals(candidate)) {
            status = CompatibilityStatus.UNVERIFIED;
            reason = "No authoritative evidence establishes direct payment rail routing from PayPal to the merchant's UPI address. " +
                    "PayPal Sandbox demonstrates an isolated payment checkout and does not transfer funds to domestic UPI VPAs.";
            limitations.add("PayPal Sandbox checkout is a separate demonstration flow and does not pay the merchant encoded in a scanned UPI QR.");
            limitations.add("Compatibility has not been confirmed.");
        } else {
            var evidenceRecordOpt = evidenceStore.findEvidence(candidate, country);

            if (evidenceRecordOpt.isEmpty()) {
                // COMP-03: No evidence available -> UNVERIFIED
                status = CompatibilityStatus.UNVERIFIED;
                reason = "Provider support has not been verified. No authoritative documentation establishes payment route compatibility for '" +
                        candidate + "' from country '" + country + "'.";
                limitations.add("Compatibility has not been confirmed. Unverified status indicates lack of documented evidence.");
            } else {
                var record = evidenceRecordOpt.get();
                evidence.addAll(record.evidenceItems());

                if (!record.activeAndValid()) {
                    // COMP-04: Evidence is expired or outdated -> UNVERIFIED
                    status = CompatibilityStatus.UNVERIFIED;
                    reason = "Documented evidence for provider '" + candidate + "' is expired or outdated; current compatibility cannot be confirmed.";
                    limitations.add("Historical or expired documentation cannot be used to verify active payment capabilities.");
                } else if (record.conflicting()) {
                    // COMP-06: Conflicting evidence sources -> UNVERIFIED
                    status = CompatibilityStatus.UNVERIFIED;
                    reason = "Conflicting evidence sources detected regarding provider cross-border routing. Unresolved discrepancies prevent verification.";
                    limitations.add("Contradictory regulatory or technical documentation requires formal clarification before support can be established.");
                } else if (!record.coversQrPaymentScenario()) {
                    // COMP-05: Capability does not cover in-person UPI QR payment -> UNVERIFIED
                    status = CompatibilityStatus.UNVERIFIED;
                    reason = "Provider capabilities do not support the UPI QR payment scenario.";
                    limitations.add("Provider support for other scenarios (such as web e-commerce) does not extend to in-person UPI QR merchant payments.");
                } else if (record.conditions() != null && !record.conditions().isBlank()) {
                    // COMP-02: Support depends on additional conditions -> CONDITIONAL
                    status = CompatibilityStatus.CONDITIONAL;
                    reason = "Route is supported conditionally: " + record.conditions();
                    limitations.add("Transaction execution requires all documented prerequisites to be satisfied by the merchant acquiring bank and payer institution.");
                } else {
                    // COMP-01: Reliable, unexpired, non-conflicting evidence establishes support -> COMPATIBLE
                    status = CompatibilityStatus.COMPATIBLE;
                    reason = "Documented bilateral linkage confirms routing support for '" + candidate + "' from " + country + " to INR UPI merchants.";
                }
            }
        }

        CompatibilityEvaluationResponse response = new CompatibilityEvaluationResponse(
                evaluationId,
                status,
                reason,
                evidence,
                limitations,
                evaluatedAt
        );

        evaluationHistory.put(evaluationId, response);
        return response;
    }

    private void validateRequest(CompatibilityEvaluationRequest request) {
        if (request == null) {
            throw new InvalidEvaluationRequestException("Request body must not be null.");
        }
        if (request.qrText() == null || request.qrText().isBlank()) {
            throw new InvalidEvaluationRequestException("qrText must not be empty or blank.");
        }
        if (request.visitorCountry() == null || request.visitorCountry().isBlank()) {
            throw new InvalidEvaluationRequestException("visitorCountry must not be empty or blank.");
        }
        if (!COUNTRY_CODE_PATTERN.matcher(request.visitorCountry().trim()).matches()) {
            throw new InvalidEvaluationRequestException("visitorCountry must be a 2-letter ISO country code (e.g., 'DE', 'SG', 'US').");
        }
        if (request.candidateProvider() == null || request.candidateProvider().isBlank()) {
            throw new InvalidEvaluationRequestException("candidateProvider must not be empty or blank.");
        }
    }

    public Optional<CompatibilityEvaluationResponse> findEvaluationById(String evaluationId) {
        if (evaluationId == null || evaluationId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(evaluationHistory.get(evaluationId.trim()));
    }

    public void saveEvaluation(CompatibilityEvaluationResponse response) {
        if (response != null && response.evaluationId() != null) {
            evaluationHistory.put(response.evaluationId(), response);
        }
    }

    public Map<String, CompatibilityEvaluationResponse> getEvaluationHistory() {
        return Collections.unmodifiableMap(evaluationHistory);
    }

    public List<CompatibilityEvaluationResponse> getAllEvaluations() {
        List<CompatibilityEvaluationResponse> list = new ArrayList<>(evaluationHistory.values());
        list.sort((a, b) -> {
            int cmp = b.evaluatedAt().compareTo(a.evaluatedAt());
            if (cmp != 0) return cmp;
            return b.evaluationId().compareTo(a.evaluationId());
        });
        return Collections.unmodifiableList(list);
    }
}
