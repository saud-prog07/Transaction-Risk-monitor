# REST API Reference

Comprehensive API endpoint documentation for the risk monitoring system.

## Base URLs

- **producer-service**: http://localhost:8080
- **risk-engine**: http://localhost:8081
- **alert-service**: http://localhost:8082

---

## producer-service API

### 1. POST /transaction

Submit a transaction for risk analysis.

**Request**:
```http
POST /transaction HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "userId": "USER123",
  "amount": 5000.00,
  "location": "New York, NY"
}
```

**Request Body** (Required Fields):
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| userId | string | @NotBlank | Unique user identifier |
| amount | decimal | @NotNull, @Positive | Transaction amount (>0) |
| location | string | @NotBlank | Geographic location (city or coordinates) |

**Response** (201 Created):
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "USER123",
  "amount": 5000.00,
  "timestamp": "2024-01-15T10:30:00Z",
  "location": "New York, NY"
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| transactionId | UUID string | Auto-generated unique identifier |
| userId | string | Echo of request userId |
| amount | decimal | Echo of request amount |
| timestamp | ISO 8601 | Server timestamp when transaction was received |
| location | string | Echo of request location |

**Error Responses**:

400 Bad Request (Validation failure):
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "message": "Validation failed",
  "detail": "Invalid request body",
  "errors": {
    "userId": "must not be blank",
    "amount": "must be positive"
  }
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER123",
    "amount": 5000.00,
    "location": "New York, NY"
  }'
```

**Flow**:
1. Validates input (userId, amount, location not blank/null)
2. Generates UUID for transactionId
3. Sets timestamp to Instant.now()
4. Publishes to IBM MQ TRANSACTION_QUEUE (with retry)
5. Returns 201 CREATED with complete transaction object

---

### 2. GET /actuator/health

Check producer-service health status.

**Request**:
```http
GET /actuator/health HTTP/1.1
Host: localhost:8080
```

**Response** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": { "total": 1000000000, "free": 500000000, "threshold": 10485760, "path": "." }
    },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" },
    "mqHealth": {
      "status": "UP",
      "details": { "queueManager": "TRANSACTION_QM", "connected": true }
    }
  }
}
```

---

## risk-engine API

### 1. POST /api/risk/analyze

Manually analyze a transaction for risk (diagnostic endpoint).

**Request**:
```http
POST /api/risk/analyze HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "USER123",
  "amount": 5000.00,
  "timestamp": "2024-01-15T10:30:00Z",
  "location": "New York, NY"
}
```

**Response** (200 OK):
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "riskLevel": "HIGH",
  "reason": "Amount is 2.5x user average; Frequency: 7 transactions in 5 minutes"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8081/api/risk/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "USER123",
    "amount": 5000.00,
    "timestamp": "2024-01-15T10:30:00Z",
    "location": "New York, NY"
  }'
```

---

### 2. GET /api/risk/analyzers

List all available risk analyzers.

**Request**:
```http
GET /api/risk/analyzers HTTP/1.1
Host: localhost:8081
```

**Response** (200 OK):
```json
[
  {
    "name": "HighAmountAnalyzer",
    "enabled": true,
    "description": "Detects transactions exceeding user average"
  },
  {
    "name": "FrequencyAnalyzer",
    "enabled": true,
    "description": "Detects unusual transaction frequency"
  },
  {
    "name": "LocationAnomalyAnalyzer",
    "enabled": true,
    "description": "Detects transactions from new locations"
  }
]
```

---

### 3. GET /api/risk/health

Health status of risk-engine service.

**Request**:
```http
GET /api/risk/health HTTP/1.1
Host: localhost:8081
```

**Response** (200 OK):
```json
{
  "status": "UP",
  "service": "risk-engine",
  "version": "1.0.0",
  "components": {
    "messageQueue": { "status": "UP", "listenersConcurrency": "1-5" },
    "database": { "status": "UP", "type": "H2", "transactions": 1234 }
  }
}
```

---

## alert-service API

### 1. POST /api/alerts

Create an alert (receive flagged transaction from risk-engine).

**Request**:
```http
POST /api/alerts HTTP/1.1
Host: localhost:8082
Content-Type: application/json

{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "USER123",
  "amount": 5000.00,
  "location": "New York, NY",
  "timestamp": "2024-01-15T10:30:00Z",
  "riskLevel": "HIGH",
  "reason": "Amount is 2.5x user average"
}
```

**Request Body fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| transactionId | UUID | @NotNull, unique | Unique transaction identifier |
| userId | string | @NotBlank | User identifier |
| amount | decimal | @NotNull, @Positive | Transaction amount |
| location | string | @NotBlank | Transaction location |
| timestamp | ISO 8601 | @NotNull | Transaction timestamp |
| riskLevel | string | @NotNull | Enum: "HIGH", "MEDIUM", "LOW" |
| reason | string | @NotBlank | Risk explanation |

**Response** (201 Created):
```json
{
  "id": 1,
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "riskLevel": "HIGH",
  "reason": "Amount is 2.5x user average",
  "createdAt": "2024-01-15T10:30:05Z",
  "updatedAt": "2024-01-15T10:30:05Z",
  "reviewed": false,
  "investigationNotes": null
}
```

**Error Responses**:

400 Bad Request:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "message": "Validation failed",
  "detail": "Invalid alert request",
  "errors": {
    "riskLevel": "must not be null"
  }
}
```

409 Conflict (Duplicate transaction):
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 409,
  "message": "Alert already exists",
  "detail": "Transaction 550e8400-... already flagged"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8082/api/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "USER123",
    "amount": 5000.00,
    "location": "New York, NY",
    "timestamp": "2024-01-15T10:30:00Z",
    "riskLevel": "HIGH",
    "reason": "Amount is 2.5x user average"
  }'
```

---

### 2. GET /api/alerts

Retrieve all flagged transactions (paginated).

**Request**:
```http
GET /api/alerts?page=0&size=20&sort=createdAt,desc HTTP/1.1
Host: localhost:8082
```

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | integer | 0 | Zero-based page number |
| size | integer | 20 | Items per page (max 100) |
| sort | string | createdAt,desc | Sort field and direction |

**Valid sort fields**: `id`, `createdAt`, `updatedAt`, `riskLevel`, `reviewed`

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 2,
      "transactionId": "660f9411-f40c-41d4-a716-446655440111",
      "riskLevel": "MEDIUM",
      "reason": "Frequency: 5 transactions in 5 minutes",
      "createdAt": "2024-01-15T10:32:10Z",
      "updatedAt": "2024-01-15T10:32:10Z",
      "reviewed": false,
      "investigationNotes": null
    },
    {
      "id": 1,
      "transactionId": "550e8400-e29b-41d4-a716-446655440000",
      "riskLevel": "HIGH",
      "reason": "Amount is 2.5x user average",
      "createdAt": "2024-01-15T10:30:05Z",
      "updatedAt": "2024-01-15T10:30:05Z",
      "reviewed": false,
      "investigationNotes": null
    }
  ],
  "pageable": {
    "sort": { "empty": false, "sorted": true, "unsorted": false },
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 20,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 2,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "sort": { "empty": false, "sorted": true, "unsorted": false },
  "first": true,
  "numberOfElements": 2,
  "empty": false
}
```

**cURL Example**:
```bash
curl "http://localhost:8082/api/alerts?page=0&size=20&sort=createdAt,desc"
```

---

### 3. GET /api/alerts/{id}

Retrieve a single alert by ID.

**Request**:
```http
GET /api/alerts/1 HTTP/1.1
Host: localhost:8082
```

**Response** (200 OK):
```json
{
  "id": 1,
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "riskLevel": "HIGH",
  "reason": "Amount is 2.5x user average",
  "createdAt": "2024-01-15T10:30:05Z",
  "updatedAt": "2024-01-15T10:30:05Z",
  "reviewed": false,
  "investigationNotes": null
}
```

**Error Response** (404 Not Found):
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "message": "Alert not found",
  "detail": "No alert with id: 999"
}
```

**cURL Example**:
```bash
curl http://localhost:8082/api/alerts/1
```

---

### 4. GET /api/alerts/by-risk-level/{level}

Filter alerts by risk level.

**Request**:
```http
GET /api/alerts/by-risk-level/HIGH?page=0&size=20 HTTP/1.1
Host: localhost:8082
```

**Path Parameters**:
| Parameter | Values | Description |
|-----------|--------|-------------|
| level | HIGH, MEDIUM, LOW | Risk level filter |

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "transactionId": "550e8400-e29b-41d4-a716-446655440000",
      "riskLevel": "HIGH",
      "reason": "Amount is 2.5x user average",
      "createdAt": "2024-01-15T10:30:05Z",
      "updatedAt": "2024-01-15T10:30:05Z",
      "reviewed": false,
      "investigationNotes": null
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**cURL Example**:
```bash
curl "http://localhost:8082/api/alerts/by-risk-level/HIGH?page=0&size=20"
```

---

### 5. GET /api/alerts/unreviewed

Retrieve unreviewed alerts (reviewed = false).

**Request**:
```http
GET /api/alerts/unreviewed?page=0&size=20 HTTP/1.1
Host: localhost:8082
```

**Query Parameters**: `page`, `size`, `sort` (same as GET /api/alerts)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 2,
      "transactionId": "660f9411-f40c-41d4-a716-446655440111",
      "riskLevel": "MEDIUM",
      "reason": "Frequency: 5 transactions in 5 minutes",
      "createdAt": "2024-01-15T10:32:10Z",
      "updatedAt": "2024-01-15T10:32:10Z",
      "reviewed": false,
      "investigationNotes": null
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**cURL Example**:
```bash
curl "http://localhost:8082/api/alerts/unreviewed?page=0&size=20"
```

---

### 6. GET /api/alerts/high-risk

Retrieve HIGH risk alerts only.

**Request**:
```http
GET /api/alerts/high-risk?page=0&size=20 HTTP/1.1
Host: localhost:8082
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "transactionId": "550e8400-e29b-41d4-a716-446655440000",
      "riskLevel": "HIGH",
      "reason": "Amount is 2.5x user average",
      "createdAt": "2024-01-15T10:30:05Z",
      "updatedAt": "2024-01-15T10:30:05Z",
      "reviewed": false,
      "investigationNotes": null
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**cURL Example**:
```bash
curl "http://localhost:8082/api/alerts/high-risk?page=0&size=20"
```

---

### 7. PUT /api/alerts/{id}/review

Mark an alert as reviewed with optional investigation notes.

**Request**:
```http
PUT /api/alerts/1/review HTTP/1.1
Host: localhost:8082
Content-Type: application/json

{
  "investigationNotes": "Verified through customer contact - legitimate bulk purchase"
}
```

**Request Body** (Optional):
| Field | Type | Description |
|-------|------|-------------|
| investigationNotes | string | Analyst investigation findings (max 2000 chars) |

**Response** (200 OK):
```json
{
  "id": 1,
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "riskLevel": "HIGH",
  "reason": "Amount is 2.5x user average",
  "createdAt": "2024-01-15T10:30:05Z",
  "updatedAt": "2024-01-15T10:31:23Z",
  "reviewed": true,
  "investigationNotes": "Verified through customer contact - legitimate bulk purchase"
}
```

**Error Response** (404 Not Found):
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "message": "Alert not found",
  "detail": "No alert with id: 999"
}
```

**cURL Example**:
```bash
curl -X PUT http://localhost:8082/api/alerts/1/review \
  -H "Content-Type: application/json" \
  -d '{
    "investigationNotes": "Verified through customer contact - legitimate bulk purchase"
  }'
```

---

### 8. GET /api/alerts/statistics

Retrieve alert statistics and metrics.

**Request**:
```http
GET /api/alerts/statistics HTTP/1.1
Host: localhost:8082
```

**Response** (200 OK):
```json
{
  "totalAlerts": 42,
  "unreviewedCount": 12,
  "highRiskCount": 8,
  "mediumRiskCount": 19
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| totalAlerts | integer | Total number of flagged transactions |
| unreviewedCount | integer | Count of alerts with reviewed = false |
| highRiskCount | integer | Count of alerts with riskLevel = HIGH |
| mediumRiskCount | integer | Count of alerts with riskLevel = MEDIUM |

**cURL Example**:
```bash
curl http://localhost:8082/api/alerts/statistics
```

---

### 9. GET /actuator/health

Check alert-service health and database connection pool status.

**Request**:
```http
GET /actuator/health HTTP/1.1
Host: localhost:8082
```

**Response** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

### 10. GET /actuator/health/databaseHealth

Detailed database connection pool metrics.

**Request**:
```http
GET /actuator/health/databaseHealth HTTP/1.1
Host: localhost:8082
```

**Response** (200 OK):
```json
{
  "status": "UP",
  "pool_size": 5,
  "active_connections": 2,
  "idle_connections": 3,
  "waiting_threads": 0
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| status | string | Health status: UP/DOWN |
| pool_size | integer | Number of connections in pool |
| active_connections | integer | Currently executing queries |
| idle_connections | integer | Available connections |
| waiting_threads | integer | Threads waiting for connection |

**cURL Example**:
```bash
curl http://localhost:8082/actuator/health/databaseHealth
```

---

## Error Response Format

All endpoints return standardized error responses:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "message": "Validation failed",
  "detail": "Additional context about the error",
  "errors": {
    "fieldName": "Specific field error message"
  }
}
```

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| timestamp | ISO 8601 | When error occurred (server time) |
| status | integer | HTTP status code (400, 404, 409, 500, etc) |
| message | string | Short error message |
| detail | string | Detailed explanation |
| errors | object | Field-level validation errors (if applicable) |

---

## Common HTTP Status Codes

| Code | Meaning | When Returned |
|------|---------|---------------|
| 200 | OK | Successful read operation |
| 201 | Created | Alert successfully created (POST) |
| 400 | Bad Request | Validation error, missing/invalid fields |
| 404 | Not Found | Alert ID doesn't exist |
| 409 | Conflict | Duplicate transaction ID |
| 500 | Server Error | Unexpected server error |

---

## Pagination

All list endpoints support pagination with standard parameters:

**Parameters**:
- `page` (int, default: 0) - Zero-based page number
- `size` (int, default: 20) - Records per page
- `sort` (string, default: "createdAt,desc") - Sort field and direction

**Response includes pagination metadata**:
```json
{
  "content": [...],
  "pageable": {...},
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0,
  "numberOfElements": 20,
  "first": true,
  "last": false,
  "empty": false
}
```

---

## Authentication (Future)

Currently, all endpoints are public. Future versions should implement:
- API Key authentication
- OAuth 2.0
- JWT tokens

---

## Rate Limiting (Future)

Consider implementing:
- 100 requests per minute per client
- 1000 requests per hour per client

---

## Example Workflows

### Workflow 1: Submit and Track Transaction

```bash
# 1. Submit transaction
curl -X POST http://localhost:8080/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER001",
    "amount": 10000.00,
    "location": "London, UK"
  }'

# Response: transactionId: 550e8400-...

# 2. Wait 2-3 seconds for risk analysis

# 3. Query alerts
curl "http://localhost:8082/api/alerts"

# 4. Find specific transaction
curl "http://localhost:8082/api/alerts" | jq '.content[] | select(.transactionId=="550e8400-...")'

# 5. Mark as reviewed
curl -X PUT http://localhost:8082/api/alerts/1/review \
  -H "Content-Type: application/json" \
  -d '{
    "investigationNotes": "Verified legitimate transaction"
  }'
```

### Workflow 2: Monitor System Health

```bash
# Check all services
for port in 8080 8081 8082; do
  echo "Port $port:"
  curl -s http://localhost:$port/actuator/health | jq '.status'
done

# Check database pool
curl http://localhost:8082/actuator/health/databaseHealth | jq '.'

# Get statistics
curl http://localhost:8082/api/alerts/statistics | jq '.'
```

### Workflow 3: Bulk Export Alerts

```bash
# Export all high-risk alerts to JSON file
curl "http://localhost:8082/api/alerts/high-risk?page=0&size=1000" | \
  jq '.content' > high_risk_alerts.json

# Export unreviewed alerts
curl "http://localhost:8082/api/alerts/unreviewed?page=0&size=1000" | \
  jq '.content' > unreviewed_alerts.json

# Count by risk level
curl "http://localhost:8082/api/alerts/statistics" | jq '.'
```

---

## Version Information

**Current Version**: 1.0.0

**Endpoint Version**: v1 (implied in /api prefix)
