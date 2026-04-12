# Transaction Trace View - Implementation Guide

**Version:** 1.0  
**Date:** April 2026  
**Status:** Production Ready

---

## Overview

The Transaction Trace View is a comprehensive feature that displays the complete lifecycle of a transaction as it moves through the fraud detection system. It provides millisecond-level timing information, service details, and processing metrics for forensics, debugging, and compliance auditing.

### User Journey
1. User navigates to an alert in the Operations Dashboard
2. Clicks on the alert to open the Alert Detail Page
3. Scrolls to the **Transaction Lifecycle Trace** section
4. Views the complete timeline: RECEIVED → QUEUED → PROCESSED → FLAGGED → ALERTED

### Key Features
- **Real-time Data**: Fetches actual transaction timing from backend
- **Visual Timeline**: Color-coded stepper showing all lifecycle stages
- **Detailed Metrics**: Processing duration, cumulative time, service information
- **Security**: JWT authentication with role-based access control
- **Responsive UI**: Chakra UI components for modern, accessible design

---

## Backend Implementation

### API Endpoint

**URL:** `GET /api/transactions/trace/{transactionId}`

**Authentication:** JWT Token Required  
**Authorization:** ANALYST or ADMIN role required

**Response Status:**
- `200 OK` - Transaction trace retrieved successfully
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - User lacks required ANALYST/ADMIN role
- `404 Not Found` - Transaction not found

### Request Example

```bash
curl -X GET http://localhost:8082/api/transactions/trace/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### Response Structure

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "us***@example.com",
  "amount": null,
  "riskLevel": "HIGH",
  "riskScore": 87.5,
  "reason": "Large amount + unusual location + time anomaly",
  "stages": [
    {
      "stage": "RECEIVED",
      "description": "Transaction received via REST API",
      "timestamp": "2026-04-09T14:30:45.123Z",
      "durationFromPreviousMs": 0,
      "cumulativeTimeMs": 0,
      "service": "producer-service",
      "statusMessage": "Transaction submitted and validated"
    },
    {
      "stage": "QUEUED",
      "description": "Transaction queued for processing",
      "timestamp": "2026-04-09T14:30:45.145Z",
      "durationFromPreviousMs": 22,
      "cumulativeTimeMs": 22,
      "service": "message-broker",
      "statusMessage": "Placed in IBM MQ for risk analysis"
    },
    {
      "stage": "PROCESSED",
      "description": "Risk engine completed analysis",
      "timestamp": "2026-04-09T14:30:46.145Z",
      "durationFromPreviousMs": 1000,
      "cumulativeTimeMs": 1022,
      "service": "risk-engine",
      "statusMessage": "Analysis complete: Large amount + unusual location + time anomaly",
      "additionalData": "Risk Level: HIGH"
    },
    {
      "stage": "FLAGGED",
      "description": "Alert flagged and stored in database",
      "timestamp": "2026-04-09T14:30:46.180Z",
      "durationFromPreviousMs": 35,
      "cumulativeTimeMs": 1057,
      "service": "alert-service",
      "statusMessage": "High-risk transaction alert created"
    },
    {
      "stage": "ALERTED",
      "description": "Alert notification sent to stakeholders",
      "timestamp": "2026-04-09T14:30:46.380Z",
      "durationFromPreviousMs": 200,
      "cumulativeTimeMs": 1257,
      "service": "notification-service",
      "statusMessage": "Alert notifications dispatched"
    }
  ],
  "totalProcessingTimeMs": 1257,
  "alertCreatedAt": "2026-04-09T14:30:46.180Z",
  "alertStatus": "NEW"
}
```

### Backend Components

#### 1. **TransactionTraceResponse.java** (DTO)
Location: `backend/alert-service/src/main/java/.../dto/TransactionTraceResponse.java`

```
Contains:
- TransactionTraceResponse: Main response object
- TraceStage: Nested class representing each lifecycle stage
  - stage: RECEIVED, QUEUED, PROCESSED, FLAGGED, ALERTED
  - description: Human-readable stage description
  - timestamp: ISO 8601 timestamp
  - durationFromPreviousMs: Time since previous stage
  - cumulativeTimeMs: Total time from start
  - service: Service that processed this stage
  - statusMessage: Optional status details
  - additionalData: Optional extra information
```

#### 2. **TransactionTraceService.java** (Service)
Location: `backend/alert-service/src/main/java/.../service/TransactionTraceService.java`

**Responsibilities:**
- Fetch FlaggedTransaction by transactionId using FlaggedTransactionRepository
- Retrieve AlertAuditLog entries for timing information
- Build TraceStage objects for each lifecycle step
- Calculate cumulative and delta timing
- Sanitize user IDs to protect PII
- Return complete TransactionTraceResponse

**Key Methods:**
```java
public TransactionTraceResponse getTransactionTrace(UUID transactionId)
  - Fetches complete transaction trace
  - Throws ResourceNotFoundException if not found

private List<TraceStage> buildTraceStages(FlaggedTransaction transaction, List<AlertAuditLog> auditLogs)
  - Constructs stages from audit logs
  - Calculates precise timing

private Long calculateTotalProcessingTime(List<TraceStage> stages)
  - Computes end-to-end processing duration

private String sanitizeUserId(String userId)
  - Masks PII (emails, IDs) for security
  - Returns "USER" if not available
```

#### 3. **TransactionTraceController.java** (Controller)
Location: `backend/alert-service/src/main/java/.../controller/TransactionTraceController.java`

**Endpoint:**
```java
@GetMapping("/{transactionId}")
@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
public ResponseEntity<TransactionTraceResponse> getTransactionTrace(UUID transactionId)
  - Requires JWT authentication (via SecurityContext)
  - Requires ANALYST or ADMIN role
  - Returns 200 with TransactionTraceResponse if successful
  - Returns 404 if transaction not found
  - Returns 403 if user lacks permissions
```

#### 4. **Security Implementation**

**Authentication:**
- Endpoint protected by Spring Security `@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")`
- User must have valid JWT token in Authorization header
- Token validated via JwtAuthenticationFilter

**Authorization:**
- Only ANALYST and ADMIN roles can access transaction traces
- Prevents unauthorized access to transaction data
- No additional service-layer checks needed (role sufficient)

**Data Protection:**
- User IDs sanitized (PII masked)
- No sensitive data exposed in response
- Timestamps only (no passwords, tokens, or card numbers)

---

## Frontend Implementation

### Component: TransactionTraceView.js

**Location:** `frontend/dashboard/src/components/TransactionTraceView.js`

**Props:**
- `transactionId` (string, optional): UUID of the transaction to display

**Features:**
- Automatically fetches trace data when transactionId changes
- Displays loading spinner during API call
- Shows error messages for failed requests (401, 403, 404, etc.)
- Renders visual timeline with Chakra UI components
- Responsive design adapts to different screen sizes

### Integration

**Used in:** `AlertDetailPage.js`

```javascript
<TransactionTraceView transactionId={alert?.transaction_id} />
```

The component receives the transaction ID from the selected alert and automatically fetches the lifecycle data.

### UI Components

#### 1. **Header**
- Transaction ID (monospace font)
- Risk Level badge (color-coded)
- Risk Score (if available)
- Alert Status badge
- Alert Reason text

#### 2. **Timeline**
- Vertical stepper showing 5 stages
- Color-coded badges for each stage
- Icons for visual recognition
  - 📥 RECEIVED (Blue)
  - ⏳ QUEUED (Yellow)
  - 🔍 PROCESSED (Green)
  - 🚩 FLAGGED (Red)
  - 🚨 ALERTED (Orange)
- Connecting lines between stages
- Timestamps for each stage
- Duration metrics
- Service information
- Optional additional data

#### 3. **Summary Section**
- Total processing time with progress bar
- Alert creation timestamp
- Current alert status
- Color-coded indicators

### API Integration

```javascript
const response = await apiClient.get(
  `/api/transactions/trace/${transactionId}`,
  {
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('access_token')}`
    }
  }
);
```

**Error Handling:**
- 404: "Transaction not found"
- 403: "You do not have permission to view this transaction"
- 401: "Authentication required. Please login again."
- Other: Backend error message

### CSS Styling

Using Chakra UI components:
- `Box`, `Container`, `VStack`, `HStack` for layout
- `Card`, `CardHeader`, `CardBody` for structure
- `Badges`, `Progress` for visual elements
- Responsive font sizes and spacing

---

## Data Flow Diagram

```
┌─────────────────┐
│ Alert Selected  │
├─────────────────┤
│  on AlertDetail │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│ Extract transaction_id  │
├─────────────────────────┤
│ Pass to TraceView       │
└────────┬────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Frontend: Fetch API Request      │
│ GET /api/transactions/trace/{id} │
│ + JWT Token in Authorization     │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ Backend: Security Check              │
│ 1. Verify JWT token                  │
│ 2. Check ANALYST/ADMIN role          │
│ 3. Verify user authorization         │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ Backend: TransactionTraceService     │
│ 1. Fetch FlaggedTransaction          │
│ 2. Fetch AlertAuditLogs              │
│ 3. Build TraceStage objects          │
│ 4. Calculate timings                 │
│ 5. Sanitize user IDs                 │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ Response: TransactionTraceResponse   │
│ - 5 Lifecycle stages                 │
│ - Timestamps & durations             │
│ - Service information                │
│ - Risk details                       │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ Frontend: Render Timeline            │
│ - Parse stages array                 │
│ - Loop through stages                │
│ - Color-code by stage type           │
│ - Display metrics                    │
│ - Format timestamps                  │
└──────────────────────────────────────┘
```

---

## Security Considerations

### 1. Authentication
- ✅ JWT token required (checked by `@PreAuthorize`)
- ✅ Token validated by Spring Security
- ✅ Token expiration enforced

### 2. Authorization
- ✅ Role-based access (ANALYST/ADMIN only)
- ✅ Prevents non-analysts from viewing transaction details
- ✅ No cross-user access (if implemented at service layer)

### 3. Data Protection
- ✅ User IDs sanitized (PII masked)
  - Email: `us***@example.com`
  - User ID: `user***`
- ✅ No sensitive data in response (passwords, tokens, card numbers)
- ✅ Response-only timestamps (immutable audit trail)

### 4. Error Handling
- ✅ Generic error messages (no stack traces exposed)
- ✅ Specific error codes for security events (401, 403)
- ✅ No information leakage about system internals

### 5. Database
- ✅ Parameterized queries (JPA/Hibernate)
- ✅ Read-only transaction (`@Transactional(readOnly = true)`)
- ✅ Indexed queries for performance (transactionId unique)

---

## Testing

### Backend Testing

**Unit Tests:**
```java
// TransactionTraceServiceTest
- testGetTransactionTrace_Success()
- testGetTransactionTrace_NotFound()
- testBuildTraceStages_CorrectSequence()
- testCalculateTotalProcessingTime()
- testSanitizeUserId_Email()
- testSanitizeUserId_UserId()
```

**Integration Tests:**
```java
// TransactionTraceControllerTest
- testGetTransactionTrace_WithValidAuth()
- testGetTransactionTrace_WithoutAuth() // 401
- testGetTransactionTrace_WithoutPermission() // 403
- testGetTransactionTrace_NotFound() // 404
- testGetTransactionTrace_InvalidUUID() // 400
```

**API Testing (cURL):**
```bash
# Valid request
curl -X GET http://localhost:8082/api/transactions/trace/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Without auth
curl -X GET http://localhost:8082/api/transactions/trace/550e8400-e29b-41d4-a716-446655440000
# Response: 401 Unauthorized

# Invalid role (USER role, not ANALYST)
curl -X GET http://localhost:8082/api/transactions/trace/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer USER_TOKEN"
# Response: 403 Forbidden

# Not found
curl -X GET http://localhost:8082/api/transactions/trace/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer YOUR_TOKEN"
# Response: 404 Not Found
```

### Frontend Testing

**Component Tests:**
```javascript
// TransactionTraceView.test.js
- render with transactionId
- fetch and display trace data
- handle loading state
- handle error states (401, 403, 404)
- format timestamps correctly
- color-code stages
- calculate durations
```

**Integration Tests:**
```javascript
// AlertDetailPage.test.js
- render TransactionTraceView when alert selected
- pass correct transactionId to TraceView
- update when alert changes
- handle missing transaction ID gracefully
```

---

## Performance Considerations

### Backend
- **Database Queries:** 1 query for FlaggedTransaction + 1 query for AuditLogs
- **Caching:** Could cache trace data for recently viewed transactions
- **Optimization:** Indexed queries on transactionId (unique), alertId
- **Read-only Transaction:** No lock contention

### Frontend
- **API Call:** Single HTTP request, ~50-100ms network time
- **Rendering:** Timeline renders ~5 stages, minimal DOM updates
- **Memoization:** Component re-renders only when transactionId changes
- **CSS-in-JS:** Chakra UI handles styling efficiently

---

## Future Enhancements

1. **Live Updates**: WebSocket integration to show real-time stage updates
2. **Export**: CSV/PDF export of transaction trace
3. **Comparison**: Side-by-side comparison of multiple traces
4. **Filtering**: Filter transactions by processing time, service, date range
5. **Analytics**: Aggregate timing statistics (p50, p95, p99 latencies)
6. **Alerts**: Trigger alerts if processing time exceeds threshold

---

## Troubleshooting

### Issue: 404 Not Found
**Solution:** Verify transaction ID is correct and alert exists in database

### Issue: 403 Forbidden
**Solution:** User role must be ANALYST or ADMIN. Check user's roles:
```sql
SELECT * FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'username');
```

### Issue: Empty Timeline
**Solution:** Check AlertAuditLog entries exist for the transaction:
```sql
SELECT * FROM alert_audit_logs WHERE alert_id = (SELECT id FROM flagged_transactions WHERE transaction_id = 'uuid');
```

### Issue: Incorrect Timestamps
**Solution:** Verify all services use UTC time (check application.yml for timezone):
```yaml
spring:
  application:
    name: alert-service
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
```

---

## References

- [Spring Security Authorization](https://docs.spring.io/spring-security/docs/current/reference/html5/#authorization)
- [JWT Authentication in Spring Boot](https://spring.io/blog/2015/01/12/the-login-page-will-no-longer-be-shown-explicitly)
- [Chakra UI Timeline/Stepper Components](https://chakra-ui.com/)
- [Transport Layer Security (TLS)](https://www.rfc-editor.org/rfc/rfc5246)

---

**Author:** Development Team  
**Last Updated:** April 9, 2026  
**Status:** Production Ready
