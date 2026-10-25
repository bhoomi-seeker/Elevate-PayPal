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
│   │   │   ├── controller/     # REST controllers (e.g., HealthController)
│   │   │   ├── dto/            # Data transfer records/objects (e.g., HealthResponse)
│   │   │   └── PayBridgeApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/paybridge/
│           ├── controller/     # Controller tests (e.g., HealthControllerTest)
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

## Verifying the Health Endpoint

### Endpoint

- **Method:** `GET`
- **URL:** `http://localhost:8081/api/health`

### Example Request (PowerShell)

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/health" -Method Get
```

### Example Request (curl)

```bash
curl -X GET http://localhost:8081/api/health
```

### Expected Response

```json
{
  "status": "UP"
}
```

HTTP Status: `200 OK`
