package com.paybridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Response payload for POST /api/compatibility/evaluate as defined in docs/API_CONTRACT.md.
 */
public record CompatibilityEvaluationResponse(
        String evaluationId,
        CompatibilityStatus status,
        String reason,
        List<EvidenceItem> evidence,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<String> limitations,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant evaluatedAt
) {
}
