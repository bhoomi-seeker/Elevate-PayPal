# PayBridge AI — Backend Service

Backend REST API for PayBridge AI built with **Java 21** and **Spring Boot**.

## Prerequisites

- **Java 21** (JDK 21) installed. Ensure `JAVA_HOME` points to your Java 21 JDK installation.
- **Maven 3.9+** (optional; the included Maven wrapper `mvnw` / `mvnw.cmd` can be used).

## Project Structure

```text
backend/
├── .mvn/wrapper/               # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/paybridge/
│   │   │   ├── controller/     # REST controllers (HealthController, QrController)
│   │   │   ├── dto/            # DTOs (HealthResponse, QrParseRequest, QrParseResponse, ErrorResponse)
│   │   │   ├── exception/      # Exception handlers (GlobalExceptionHandler, InvalidQrException)
│   │   │   ├── service/        # Business logic services (QrParserService)
│   │   │   └── PayBridgeApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/paybridge/
│           ├── controller/     # Controller tests (HealthControllerTest, QrControllerTest)
│           ├── service/        # Service unit tests (QrParserServiceTest)
│           └── PayBridgeApplicationTests.java
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Running Tests

From the `backend/` directory:

```bash
# Using Maven Wrapper (Windows PowerShell)
.\mvnw.cmd clean test

# Using installed Maven
mvn clean test
```

## Running the Application

From the `backend/` directory:

```bash
# Using Maven Wrapper (Windows PowerShell)
.\mvnw.cmd spring-boot:run

# Using installed Maven
mvn spring-boot:run
```

The application starts on port `8081` by default (configurable via `PORT` environment variable or `application.properties`).

## Available Endpoints

### 1. Health Check

- **Method:** `GET`
- **URL:** `http://localhost:8081/api/health`

#### Example Request (PowerShell)

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/health" -Method Get
```

#### Expected Response

```json
{
  "status": "UP"
}
```

HTTP Status: `200 OK`

---

### 2. Parse UPI QR Payload

Parses a UPI payment QR payload and extracts the fields that are actually present.

- **Method:** `POST`
- **URL:** `http://localhost:8081/api/qr/parse`
- **Content-Type:** `application/json`

#### Example Request (PowerShell)

```powershell
$body = @{ qrText = "upi://pay?pa=merchant@example&cu=INR" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/api/qr/parse" -Method Post -Body $body -ContentType "application/json" | ConvertTo-Json -Depth 5
```

#### Example Request (curl)

```bash
curl -X POST http://localhost:8081/api/qr/parse \
  -H "Content-Type: application/json" \
  -d '{"qrText": "upi://pay?pa=merchant@example&cu=INR"}'
```

#### Expected Response

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

HTTP Status: `200 OK`

#### Error Response (e.g. malformed or invalid input)

```json
{
  "timestamp": "2026-10-25T10:00:00Z",
  "status": 400,
  "error": "INVALID_REQUEST",
  "message": "Invalid amount (am): Value must be greater than zero.",
  "path": "/api/qr/parse"
}
```

HTTP Status: `400 Bad Request`

> **Payment & Security Disclaimer:** Parsing extracts present query parameters only. Parsing does not verify merchant identity, does not prove payment route compatibility, and never initiates or executes a payment.
