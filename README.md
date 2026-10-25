# PayBridge AI 2.0

**AI-Powered Payment Compatibility & Intelligence Platform**

PayBridge AI is a project designed to evaluate payment-route compatibility using structured evidence, explain evaluation results with AI, and provide a dashboard for investigating evaluations and payment test outcomes.

> **Project status:** Under development. Features described here are planned and are not yet claimed to be implemented.

## Problem

International visitors may encounter payment methods and QR codes that their preferred payment providers cannot use. It can be difficult to distinguish a supported route from one that is conditional or has not been verified.

## Proposed Solution

PayBridge AI aims to:

- Parse supported UPI QR data and display available fields.
- Evaluate candidate payment routes using documented capabilities and evidence.
- Classify results as `COMPATIBLE`, `CONDITIONAL`, or `UNVERIFIED`.
- Use AI to explain structured evaluation results and their limitations.
- Provide an interactive dashboard for examining evaluation records and test outcomes.
- Demonstrate the PayPal Orders API using PayPal Sandbox.

## Planned Technology Stack

| Component | Technology |
|---|---|
| Frontend | React, Vite, Tailwind CSS |
| Backend | Java, Spring Boot |
| Database | MySQL |
| AI explanations | Gemini API |
| Payment demonstration | PayPal Sandbox |
| Data dashboard | AG Grid |
| Version control | Git and GitHub |

## Important Payment Limitations

- A parsed QR code does not, by itself, verify the merchant's identity or legitimacy.
- Route compatibility claims must be supported by evidence; unknown support will be labelled `UNVERIFIED`.
- PayPal Sandbox checkout is a separate payment-flow demonstration. It does not transfer funds to the merchant encoded in a scanned UPI QR code.
- AI explanations will not independently authorize payments or override backend compatibility decisions.
- This project does not claim that PayPal is accepted by every UPI QR merchant.

## Project Structure

```text
PayBridge-AI/
├── frontend/
├── backend/
├── docs/
│   ├── API_CONTRACT.md
│   ├── ARCHITECTURE.md
│   └── TEST_PLAN.md
├── .gitignore
├── LICENSE
└── README.md
```

The frontend and backend directories will be added as implementation begins.

## Getting Started

Setup instructions will be added as the frontend and backend become runnable.

## Testing

Test instructions and verified results will be documented in `docs/TEST_PLAN.md` as features are implemented.

## Open Source

This project is intended to be released under the MIT License. See `LICENSE` for the applicable terms.

## Contributors

- Member 1 — Frontend development
- Member 2 — Backend development

## Disclaimer

PayBridge AI is an independent hackathon project. It is not an official PayPal product and does not establish payment-provider support or guarantee successful transactions.