# PayBridge AI — Test Plan

**Status:** Initial draft  
**Project:** PayBridge AI  
**Purpose:** Verify functional correctness, security, reliability, and integration across the application.

## 1. Testing Objectives

The test plan will verify that:

- UPI QR data is parsed and validated correctly.
- Compatibility decisions follow deterministic backend rules.
- Insufficient evidence produces an `UNVERIFIED` result.
- AI explanations accurately reflect saved evaluation results.
- PayPal Sandbox orders follow the expected approval and capture lifecycle.
- Evaluation records appear correctly in the dashboard.
- Invalid requests and external-service failures are handled safely.

All test results and performance metrics must be based on tests actually executed.

## 2. Testing Scope

### In Scope

- QR parsing and input validation.
- Compatibility rules and evidence handling.
- AI-generated explanations.
- Evaluation history and audit events.
- PayPal Sandbox order creation, approval, and capture.
- Frontend/backend API integration.
- Dashboard filtering and sorting.
- Security and error handling.

### Out of Scope

- Real-money payment settlement.
- Guaranteed compatibility with every international payment provider.
- Production payment processing.
- Verification of a merchant's legal identity from QR contents alone.
- Claims of production-scale performance without appropriate infrastructure and testing.

## 3. Unit Tests

### 3.1 QR Parser

| Test ID | Scenario | Expected Result |
|---|---|---|
| QR-01 | Valid UPI payload with payee address and currency | Extract the fields present |
| QR-02 | Payload includes an amount | Extract the amount accurately |
| QR-03 | Optional fields are missing | Keep missing fields absent or null |
| QR-04 | Empty input | Return a validation error |
| QR-05 | Malformed URI | Reject or flag invalid input |
| QR-06 | Special characters in query values | Decode safely and correctly |
| QR-07 | Unexpected or oversized input | Reject safely according to configured limits |

### 3.2 Compatibility Engine

| Test ID | Scenario | Expected Result |
|---|---|---|
| COMP-01 | Reliable evidence establishes support for the evaluated conditions | Return `COMPATIBLE` only when all required conditions are satisfied |
| COMP-02 | Support depends on additional conditions | Return `CONDITIONAL` and identify those conditions |
| COMP-03 | No reliable evidence is available | Return `UNVERIFIED` |
| COMP-04 | Evidence is expired or outdated | Do not treat it as current verification |
| COMP-05 | Provider capability does not cover the QR payment scenario | Do not return `COMPATIBLE` |
| COMP-06 | Evidence sources conflict | Flag uncertainty and avoid unsupported conclusions |
| COMP-07 | Same input and same evidence version are evaluated again | Produce a consistent decision under unchanged rules |

Tests must use controlled fixtures and documented evidence. A test fixture does not establish real-world provider support.

### 3.3 AI Explanation Service

| Test ID | Scenario | Expected Result |
|---|---|---|
| AI-01 | Explain a saved `COMPATIBLE` evaluation | Explanation matches the recorded decision and evidence |
| AI-02 | Explain a `CONDITIONAL` evaluation | Conditions and limitations are described |
| AI-03 | Explain an `UNVERIFIED` evaluation | Uncertainty is clearly communicated |
| AI-04 | Gemini API is unavailable | Return a controlled error or fallback |
| AI-05 | Evaluation ID does not exist | Return a not-found response |
| AI-06 | Untrusted text attempts to change the decision | Preserve the backend's authoritative status |
| AI-07 | AI produces unsupported claims | Detect through tests or review and prevent those claims from being presented as verified facts |

The AI service explains existing decisions. It must not independently authorize payments or change the compatibility status.

## 4. API Integration Tests

| Test ID | Endpoint | Expected Result |
|---|---|---|
| API-01 | `GET /api/health` | Returns a healthy response when the backend is ready |
| API-02 | `POST /api/qr/parse` | Returns parsed fields or a validation error |
| API-03 | `POST /api/compatibility/evaluate` | Returns a valid status and associated evidence |
| API-04 | `POST /api/ai/explain` | Explains an existing evaluation |
| API-05 | `GET /api/evaluations` | Returns saved evaluation records |
| API-06 | `POST /api/payments/orders` | Creates a Sandbox order or returns a controlled error |
| API-07 | `POST /api/payments/orders/{orderId}/capture` | Captures only an eligible approved Sandbox order |
| API-08 | Invalid JSON request | Returns a consistent error response |
| API-09 | Unknown resource ID | Returns a not-found response where applicable |
| API-10 | External service timeout | Returns a controlled failure without claiming success |

## 5. PayPal Sandbox Tests

| Test ID | Scenario | Expected Result |
|---|---|---|
| PAY-01 | Valid Sandbox order request | Backend creates an order |
| PAY-02 | Buyer approves an order | Application receives or retrieves the correct order state |
| PAY-03 | Approved order is captured | Backend verifies PayPal's response |
| PAY-04 | Invalid order ID is supplied | Capture is rejected |
| PAY-05 | Order has not been approved | Capture is not falsely reported as successful |
| PAY-06 | Capture request is repeated | Duplicate processing is prevented or handled safely |
| PAY-07 | PayPal API is unavailable | Application reports failure or an appropriate pending state |
| PAY-08 | Secret credentials are inspected in frontend assets | No secret credentials are exposed |

Use PayPal Sandbox accounts and test credentials only. These tests do not verify real-money settlement to a UPI merchant.

## 6. Frontend Tests

- QR input accepts valid input and displays validation errors.
- Loading indicators appear during API requests.
- Compatibility statuses are displayed consistently.
- Missing evidence is clearly distinguished from confirmed support.
- AI explanations remain associated with the correct evaluation.
- Empty dashboard history is handled gracefully.
- AG Grid sorting and filtering work with returned records.
- Failed API requests show useful error messages.
- PayPal approval and return flows display the backend-verified status.
- The layout remains usable on desktop and mobile screens.

## 7. Security Tests

- API secrets are not committed to GitHub.
- Requests with invalid or oversized input are rejected.
- QR content and external data are treated as untrusted.
- AI-generated text cannot override backend decisions.
- Payment capture requires a valid eligible order.
- Error messages do not expose credentials or stack traces.
- Database queries use safe parameterization through the chosen persistence framework.
- Cross-origin settings are restricted to the intended frontend origins in deployed environments.
- Sensitive information is not unnecessarily stored in logs.

## 8. End-to-End Test Scenarios

### Scenario A — QR Evaluation

1. Open the application.
2. Enter or upload a supported UPI QR.
3. Submit the QR for parsing.
4. Review the extracted fields.
5. Evaluate a candidate provider.
6. Review the status, evidence, and limitations.
7. Confirm the evaluation appears in dashboard history.

**Expected outcome:** The displayed result matches the backend response and does not overstate provider compatibility.

### Scenario B — AI Explanation

1. Select an existing evaluation.
2. Request an explanation.
3. Compare the explanation with the stored status and evidence.
4. Simulate an AI service failure.

**Expected outcome:** The explanation preserves the original decision, and the evaluation remains available if AI fails.

### Scenario C — PayPal Sandbox Checkout

1. Request a Sandbox order.
2. Open the returned approval URL.
3. Approve the order using a Sandbox buyer account.
4. Return to the application.
5. Request capture through the backend.
6. Verify the returned order status.

**Expected outcome:** Success is displayed only after the backend verifies the relevant PayPal response.

### Scenario D — Dashboard Inspection

1. Generate several test evaluations.
2. Open the dashboard.
3. Sort and filter records by status or provider.
4. Inspect the event and evaluation details.

**Expected outcome:** The dashboard reflects saved records and actual test results.

## 9. Test Data and Environment

Use separate development and testing configuration wherever practical.

Required test data:

- Valid sample UPI payloads.
- Malformed and incomplete payloads.
- Controlled provider-evidence fixtures.
- Evaluations with all three compatibility statuses.
- PayPal Sandbox order IDs from actual test transactions.
- Simulated external-service failures.

Never use real payment credentials or real customer payment data in test fixtures.

## 10. Test Execution and Reporting

For each test, record:

- Test ID.
- Date and environment.
- Input or scenario.
- Expected result.
- Actual result.
- Status: `PASS`, `FAIL`, or `BLOCKED`.
- Notes or defect reference.

Do not mark a test as passed before executing it.

### Project Metrics

The team may report the following after running the test suite:

- Number of tests executed.
- Number of tests passed and failed.
- Pass rate.
- Compatibility decision consistency on controlled fixtures.
- API response times measured under a documented test setup.
- Number of handled external-service failures.

Pass rate formula:

`Pass rate = (Passed tests / Executed tests) × 100`

Blocked or unexecuted tests must not be counted as passed.

## 11. Definition of Done

A feature is ready for integration when:

- Its expected behavior is implemented.
- Relevant unit tests pass.
- API request and response shapes match `API_CONTRACT.md`.
- Validation and error handling are implemented.
- No known critical secret exposure remains.
- The feature has been reviewed by the other team member.
- Integration tests pass where applicable.
- Documentation is updated if the contract or behavior changes.

**Final principle:** PayBridge AI must communicate uncertainty honestly. A successful demo must not imply that a Sandbox payment paid a merchant's UPI QR or that unverified provider support has been confirmed.
