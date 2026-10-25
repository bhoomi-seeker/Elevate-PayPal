# PayBridge AI — System Architecture

**Project:** PayBridge AI  
**Tagline:** AI-Powered Payment Compatibility & Intelligence Platform  
**Status:** Proposed architecture

## 1. System Overview

PayBridge AI is a web application that evaluates documented payment-provider compatibility for a merchant's UPI QR code.

The application parses QR data, evaluates candidate payment routes using deterministic backend rules and available evidence, and provides AI-generated explanations of the results.

A separate PayPal Sandbox integration demonstrates a test payment lifecycle. It does not settle money to the merchant encoded in the scanned UPI QR.

## 2. Technology Stack

| Component | Technology | Responsibility |
|---|---|---|
| Frontend | React + Vite | User interface and client-side navigation |
| Styling | Tailwind CSS | Responsive interface and component styling |
| Backend | Java + Spring Boot | API endpoints, validation, business rules |
| Database | MySQL | Evaluation history and audit records |
| AI service | Gemini API | Explain structured evaluation results |
| Payment integration | PayPal Orders API Sandbox | Demonstrate test order approval and capture |
| Data dashboard | AG Grid | Filter, sort, and inspect evaluation records |
| Testing | JUnit, Spring Boot tests, frontend tests | Validate business rules and integration |
| Version control | Git + GitHub | Collaboration and source control |

AG Grid will provide the data-grid interface. Any additional AG Studio functionality will be assessed separately for API compatibility, licensing, and implementation feasibility.

## 3. High-Level Architecture

```text
                    USER
                     |
                     v
          REACT + VITE FRONTEND
          - QR input or upload
          - Compatibility results
          - AI explanations
          - PayPal Sandbox UI
          - AG Grid dashboard
                     |
                     | HTTPS / JSON
                     v
             SPRING BOOT API
                     |
          +----------+-----------+
          |          |           |
          v          v           v
     QR PARSER   COMPATIBILITY   PAYMENT
                 ENGINE          SERVICE
                     |              |
                     v              v
               EVIDENCE STORE   PAYPAL SANDBOX
                     |
          +----------+-----------+
          |                      |
          v                      v
      MYSQL DATABASE         GEMINI API
      - Evaluations          - Explanations
      - Audit events         - No authorization
      - Test outcomes        - No payment control
```

This diagram represents the planned design. Components will become operational as they are implemented and integrated.

## 4. Frontend Architecture

The React frontend will contain the following main pages or components:

### 4.1 QR Input

- Accept QR text or an uploaded QR image.
- Decode an image locally or through a suitable QR-decoding library.
- Validate input before sending it to the backend.
- Display parsing errors clearly.

### 4.2 Compatibility Results

- Display extracted QR fields.
- Show each candidate provider's compatibility status.
- Display supporting evidence, limitations, and evaluation time.
- Distinguish verified information from unknown or conditional results.

### 4.3 AI Explanation

- Request an explanation for an existing evaluation.
- Display the explanation alongside the original backend decision.
- Preserve the original result if the AI service fails.

### 4.4 PayPal Sandbox

- Start a Sandbox checkout.
- Redirect the user to PayPal for approval where required.
- Send the approved order identifier to the backend for capture.
- Display the verified result returned by the backend.

### 4.5 Evaluation Dashboard

Use AG Grid to display evaluation records and test outcomes.

Planned columns include:

- Evaluation ID
- Candidate provider
- Compatibility status
- Evidence availability
- Evaluation timestamp
- Sandbox order status, where applicable

Filters and metrics must operate on actual stored records.

## 5. Backend Architecture

The Spring Boot backend will expose the endpoints defined in `API_CONTRACT.md`.

### QR Parsing Service

- Parse supported UPI QR payloads.
- Extract only fields that are present.
- Validate supported formats.
- Report malformed or unsupported input.

### Compatibility Engine

- Evaluate candidate routes using explicit rules.
- Consult documented provider capabilities and evidence.
- Return `COMPATIBLE`, `CONDITIONAL`, or `UNVERIFIED`.
- Default to `UNVERIFIED` when reliable evidence is insufficient.
- Record the reason and evidence used for each evaluation.

### AI Explanation Service

- Retrieve an existing evaluation.
- Pass structured results and evidence to Gemini.
- Generate a concise explanation with limitations.
- Never allow AI output to override compatibility rules or payment state.

### PayPal Payment Service

- Create Sandbox orders using backend credentials.
- Return the order ID and approval URL.
- Validate order state before capture.
- Verify PayPal's response before recording a completed status.
- Keep payment credentials out of frontend code and GitHub.

### Evaluation and Audit Service

- Store evaluation records and relevant events.
- Record rule outcomes and external-service failures.
- Provide records for the dashboard and testing reports.

## 6. Database Design

The initial MySQL schema is expected to include these tables:

### `evaluations`

Stores compatibility evaluations.

Suggested fields:

- `id` — primary key
- `evaluation_id` — unique public identifier
- `candidate_provider` — evaluated provider
- `visitor_country` — country code supplied for the evaluation
- `compatibility_status` — result from the compatibility engine
- `reason` — decision explanation
- `evidence_json` — evidence references and metadata
- `created_at` — creation timestamp

### `audit_events`

Stores important application events.

Suggested fields:

- `id` — primary key
- `evaluation_id` — associated evaluation, when applicable
- `event_type` — event category
- `details_json` — structured event details
- `created_at` — event timestamp

### `sandbox_payments`

Stores Sandbox order records.

Suggested fields:

- `id` — primary key
- `paypal_order_id` — PayPal order identifier
- `amount` — test order amount
- `currency` — order currency
- `status` — verified order state
- `created_at` — creation timestamp
- `updated_at` — last update timestamp

The final schema, relationships, indexes, and migration strategy will be agreed upon during backend implementation. Never store PayPal secrets or access tokens in these tables.

## 7. Security and Trust Boundaries

- Validate all user input on the backend.
- Treat QR contents and external provider information as untrusted input.
- Do not treat QR parsing as proof of merchant identity.
- Store API keys in environment variables or an appropriate secrets manager.
- Do not expose Gemini or PayPal credentials to the frontend.
- Never let the AI service authorize or capture payments.
- Validate order ownership and state before capture.
- Avoid logging secrets, access tokens, or unnecessary personal information.
- Use HTTPS in deployed environments.
- Apply request validation, sensible size limits, and error handling.

Authentication and authorization requirements will be defined before adding protected user-specific features.

## 8. Error Handling and Resilience

The application should handle:

- Invalid or unreadable QR input.
- Missing or insufficient compatibility evidence.
- Database connection failures.
- Gemini API timeouts or errors.
- PayPal Sandbox API failures.
- Invalid payment-order states.
- Empty evaluation history.

When an external service fails, display a clear error and preserve any valid results already obtained. Do not fabricate a successful evaluation or payment.

## 9. Deployment Plan

The planned deployment consists of:

- Frontend: a static hosting platform supporting React builds.
- Backend: a hosting environment supporting Java and Spring Boot.
- Database: a managed MySQL instance or another appropriately secured MySQL deployment.
- External services: Gemini API and PayPal Sandbox.

Production credentials must be configured securely on the backend hosting platform. The deployed application must use the correct backend API URL and must not include private credentials in frontend build variables.

## 10. Team Responsibilities

**Member 1 — Frontend**

- React and Tailwind CSS interface.
- QR input and results display.
- API client and loading/error states.
- AG Grid dashboard.
- Frontend tests and responsive layout.

**Member 2 — Backend**

- Spring Boot API.
- QR parsing and compatibility engine.
- MySQL schema and persistence.
- Gemini explanation integration.
- PayPal Sandbox order lifecycle.
- Backend tests and API validation.

**Shared responsibilities**

- Maintain `API_CONTRACT.md`.
- Review and merge pull requests.
- Test the complete frontend-to-backend flow.
- Review security and evidence quality.
- Prepare documentation, deployment, and demonstration.

## 11. Key Limitations

- Compatibility results are only as reliable as their documented evidence.
- `UNVERIFIED` means support has not been established; it does not necessarily mean a provider is incompatible.
- AI explanations are informational and cannot change deterministic backend decisions.
- PayPal Sandbox transactions are test transactions and do not pay the UPI merchant encoded in a QR.
- The architecture describes intended functionality, not features already implemented.
