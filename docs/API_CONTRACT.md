# PayBridge AI — API Contract

**Status:** Initial draft  
**Backend:** Java + Spring Boot  
**Frontend:** React + Vite

This document defines the planned interface between the frontend and backend. Both team members must agree on changes before implementation diverges.

## General Rules

- API base path: `/api`
- Request and response format: JSON, except QR file upload.
- The backend is authoritative for compatibility decisions and payment status.
- All timestamps should use ISO 8601 format with a timezone.
- Error responses must use a consistent structure.
- Never send API secrets to the frontend.

## 1. Health Check

### `GET /api/health`

Checks whether the backend is responding.

Example response:

```json
{
  "status": "UP"
}
```

## 2. Parse UPI QR Data

### `POST /api/qr/parse`

Parses a UPI QR payload and extracts the fields that are actually present.

Request:

```json
{
  "qrText": "upi://pay?pa=merchant@example&cu=INR"
}
```

Example response:

```json
{
  "validFormat": true,
  "fields": {
    "pa": "merchant@example",
    "pn": null,
    "am": null,
    "cu": "INR",
    "tn": null
  },
  "warnings": []
}
```

Only fields present in the QR should be returned as values. Missing fields must not be invented. Parsing does not establish merchant identity or legitimacy.

## 3. Evaluate Candidate Compatibility

### `POST /api/compatibility/evaluate`

Evaluates a candidate payment provider using documented capabilities and available evidence.

Request:

```json
{
  "qrText": "upi://pay?pa=merchant@example&cu=INR",
  "visitorCountry": "DE",
  "candidateProvider": "example-provider"
}
```

Example response:

```json
{
  "evaluationId": "EVAL-001",
  "status": "UNVERIFIED",
  "reason": "Provider support has not been verified.",
  "evidence": [],
  "evaluatedAt": "2026-10-25T10:00:00Z"
}
```

Allowed status values:

- `COMPATIBLE`
- `CONDITIONAL`
- `UNVERIFIED`

The example provider is fictional. Real compatibility results must be determined by documented evidence and backend rules. If evidence is insufficient, the result must remain `UNVERIFIED`.

## 4. Explain an Evaluation

### `POST /api/ai/explain`

Generates a human-readable explanation of an existing evaluation.

Request:

```json
{
  "evaluationId": "EVAL-001"
}
```

Example response:

```json
{
  "evaluationId": "EVAL-001",
  "explanation": "Provider support has not been verified. Further evidence is required.",
  "limitations": [
    "Compatibility has not been confirmed."
  ]
}
```

The backend retrieves the evaluation and supplies its structured status, reason, and evidence to the AI service.

The AI explanation must not invent provider support, change the compatibility status, or authorize a payment. If the AI service is unavailable, the application should still display the original evaluation.

## 5. Retrieve Evaluation History

### `GET /api/evaluations`

Returns saved evaluation records for the dashboard and AG Grid.

Example response:

```json
{
  "items": [],
  "total": 0
}
```

Pagination, sorting, and filters will be agreed upon before implementation. The dashboard must display actual stored records rather than fabricated metrics.

## 6. Create a PayPal Sandbox Order

### `POST /api/payments/orders`

Creates a test order through the PayPal Sandbox API.

Request:

```json
{
  "amount": "5.00",
  "currency": "USD"
}
```

Example response:

```json
{
  "orderId": "SANDBOX_ORDER_ID",
  "status": "CREATED",
  "approvalUrl": "https://example.com/approval"
}
```

The response values above are placeholders, not real PayPal credentials or order details. The actual approval URL and order ID must come from the PayPal Sandbox API.

The backend validates the amount and currency, creates the order, and returns the order identifier and approval information required by the frontend.

The frontend must not create or capture orders using secret credentials.

## 7. Capture an Approved Sandbox Order

### `POST /api/payments/orders/{orderId}/capture`

Captures an approved Sandbox order when permitted by the PayPal API.

The backend retrieves and validates the relevant order state, captures the approved order as appropriate, and verifies the returned status before reporting success.

Example response:

```json
{
  "orderId": "SANDBOX_ORDER_ID",
  "status": "COMPLETED"
}
```

The response must distinguish completed, pending, failed, and cancelled flows where applicable. The backend must not report a successful payment without verifying the actual PayPal response.

**Important:** This Sandbox order is separate from any UPI QR being evaluated. It does not pay the merchant encoded in that QR.

## Error Format

All endpoints should return a consistent error structure.

Example:

```json
{
  "timestamp": "2026-10-25T10:00:00Z",
  "status": 400,
  "error": "INVALID_REQUEST",
  "message": "The supplied QR data is invalid.",
  "path": "/api/qr/parse"
}
```

Common HTTP status codes:

- `200` — Request processed successfully.
- `201` — Resource created successfully.
- `400` — Invalid request.
- `404` — Requested resource not found.
- `409` — Request conflicts with the current resource state.
- `500` — Unexpected server error.
- `502` — External service returned an unusable response.

The backend must not expose API keys, access tokens, stack traces, or other secrets in error messages.

## Integration Agreement

- **Member 1:** Owns the frontend implementation, API client, and UI error states.
- **Member 2:** Owns the backend endpoints, validation, business rules, and API tests.
- Both members review changes to this contract.
- Frontend mock data must match these response shapes.
- API contract changes must be communicated before merging dependent features into `main`.
- Credentials and environment-specific configuration must remain outside source control.

## Important Limitations

- These endpoints are proposed application interfaces; they must be implemented and tested.
- Compatibility decisions depend on reliable, documented provider evidence.
- A valid QR format does not prove that the merchant is legitimate.
- AI explains structured results; deterministic backend rules control compatibility and payment state.
- PayPal Sandbox demonstrates a test payment lifecycle, not settlement to a merchant identified by a UPI QR.

