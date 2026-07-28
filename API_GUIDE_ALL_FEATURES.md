# Complete API Guide: All New Features

This guide covers all three newly implemented features:
1. **30-Minute Activity-Based Session Timeout**
2. **Meter Reading During Fuel Approval**
3. **Meter Reading During Tank Refill**

---

## Feature 1: Activity-Based Session Timeout (30 Minutes)

### Automatic Session Management

Your session automatically expires after 30 minutes of inactivity.

#### Login Endpoint
**POST** `/api/auth/login`

Request:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "username": "john_doe",
  "role": "DRIVER",
  "message": "Login successful"
}
```

**Token includes:** `lastActivity` timestamp (when logged in)

#### Using Your Token

Every API request requires the token in the Authorization header:

```bash
GET /api/fuel/records
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

#### Automatic Token Refresh

When your request succeeds, server sends back a refreshed token:

**Response Header:**
```
X-New-Token: eyJhbGciOiJIUzUxMiJ9...REFRESHED
```

**Client should:**
1. Extract the `X-New-Token` header
2. Store it in place of the old token
3. Use the new token for next request

This keeps your session alive as long as you're active.

#### Session Expiration After 30 Minutes Inactivity
If you don't make any requests for 30+ minutes:

```bash
GET /api/fuel/records
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...OLD

Response: 401 Unauthorized
{
  "error": "Session expired due to inactivity"
}
```

**You must log in again.**

#### Manual Token Refresh

**POST** `/api/auth/refresh`

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer {current_token}"
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...REFRESHED",
  "message": "Token refreshed successfully"
}
```

Use this endpoint if you want to manually refresh before making requests.

---

## Feature 2: Meter Reading During Fuel Approval

Authorize to record fuel tank gauge levels when approving fuel requests.

### Approval Workflow Stages

#### Stage 1: Manager Approval
**PATCH** `/api/fuel/{id}/status`

With Meter Reading (optional):
```json
{
  "status": "MANAGER_APPROVED",
  "meterReading": 450.00
}
```

Response:
```json
{
  "success": true,
  "message": "Record approved by Manager — awaiting Attendant approval"
}
```

#### Stage 2: Attendant Approval (Fuel Dispensed)
**PATCH** `/api/fuel/{id}/status`

With Meter Reading (optional):
```json
{
  "status": "ATTENDANT_APPROVED",
  "meterReading": 445.50
}
```

Response:
```json
{
  "success": true,
  "message": "Approved by Attendant — 50.00L petrol dispensed from tank",
  "fuel_type": "petrol",
  "tank_id": 1,
  "liters_deducted": 50.00,
  "current_level": 445.50,
  "percent_full": 45
}
```

#### Stage 3: Finance Sign-off
**PATCH** `/api/fuel/{id}/status`

With Meter Reading (optional):
```json
{
  "status": "SIGNED",
  "meterReading": 445.00
}
```

Response:
```json
{
  "success": true,
  "message": "Record signed by Finance"
}
```

#### Decline Request (Any Stage)
**PATCH** `/api/fuel/{id}/status`

```json
{
  "status": "DECLINED",
  "meterReading": 450.00
}
```

Response:
```json
{
  "success": true,
  "message": "Record declined"
}
```

### Complete Approval Examples

**Example 1: Approve Without Meter Reading**
```bash
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"status": "MANAGER_APPROVED"}'
```

**Example 2: Approve With Meter Reading**
```bash
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ATTENDANT_APPROVED",
    "meterReading": 445.50
  }'
```

---

## Feature 3: Meter Reading During Tank Refill

Record fuel tank gauge levels when refilling tanks.

### Refill Tank Endpoint

**POST** `/api/fuel/tank/{id}/refill`

#### With Meter Reading (Optional)

```json
{
  "liters": 500.00,
  "meterReading": 350.25
}
```

Response:
```json
{
  "success": true,
  "message": "Tank refilled successfully",
  "current_level": 850.25,
  "capacity": 1000,
  "percent_full": 85
}
```

#### Without Meter Reading

```json
{
  "liters": 500.00
}
```

Response:
```json
{
  "success": true,
  "message": "Tank refilled successfully",
  "current_level": 850.25,
  "capacity": 1000,
  "percent_full": 85
}
```

### Refill Examples

**Example 1: Refill Petrol Tank with Meter Reading**
```bash
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 500.00,
    "meterReading": 350.25
  }'
```

**Example 2: Refill Diesel Tank with Meter Reading**
```bash
curl -X POST http://localhost:8080/api/fuel/tank/2/refill \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 300.00,
    "meterReading": 200.50
  }'
```

**Example 3: Refill Without Recording Meter**
```bash
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"liters": 500.00}'
```

---

## Complete API Usage Scenario

Here's a complete workflow using all features:

### Step 1: Login (Get Initial Token)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "manager@example.com",
    "password": "password123"
  }'

# Response:
# {
#   "token": "eyJ...ABC123...",
#   "username": "john_manager",
#   "role": "MANAGER"
# }

TOKEN="eyJ...ABC123..."
```

### Step 2: Refill Tank (Record Gauge Level)

```bash
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 500.00,
    "meterReading": 350.00
  }'

# Response includes X-New-Token header
# Extract and update: TOKEN="new_token_value"

# This records:
# - 500 liters added to tank
# - Gauge reading was 350L before refill
```

### Step 3: Approve Fuel Request (Record Gauge at Approval)

```bash
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "MANAGER_APPROVED",
    "meterReading": 845.00
  }'

# Response includes X-New-Token header
# Extract and update: TOKEN="new_token_value"

# This records:
# - Approval at MANAGER stage
# - Gauge reading was 845L at time of approval
```

### Step 4: Check Session Status

Make any request to keep session alive:

```bash
curl -X GET http://localhost:8080/api/fuel/records \
  -H "Authorization: Bearer $TOKEN"

# Response includes X-New-Token header
# Extract and update: TOKEN="new_token_value"

# Your lastActivity is reset
# Session will remain valid for another 30 minutes
```

### Step 5: Handle Session Timeout

After 30 minutes of inactivity:

```bash
curl -X GET http://localhost:8080/api/fuel/records \
  -H "Authorization: Bearer $TOKEN"

# Response: 401 Unauthorized
# {
#   "error": "Session expired due to inactivity"
# }

# You must login again
```

---

## Summary: Feature Comparison

| Feature | Endpoint | Method | Optional Field | Use Case |
|---------|----------|--------|-----------------|----------|
| **Session Timeout** | Any authenticated endpoint | Auto | N/A | Security: Auto-logout after 30 min inactivity |
| **Approval Meter** | `/api/fuel/{id}/status` | PATCH | `meterReading` | Track tank level during approval |
| **Refill Meter** | `/api/fuel/tank/{id}/refill` | POST | `meterReading` | Track gauge reading during refill |

---

## Field Specifications

### All Meter Reading Fields

All meter reading fields follow the same specification:

- **JSON Field Name:** `meterReading`
- **Type:** Number (decimal)
- **Format:** `123.45` (with 2 decimal places)
- **Range:** 0.00 - 9999.99 liters
- **Required:** No (all are optional)
- **Database Type:** DECIMAL(10, 2)

### Examples of Valid Values

```json
{
  "meterReading": 0
}
```

```json
{
  "meterReading": 450.25
}
```

```json
{
  "meterReading": 999.99
}
```

```json
{
  "meterReading": 9999.99
}
```

### Invalid Examples (Will be Ignored)

```json
{
  "meterReading": "not_a_number"  // String - ignored
}
```

```json
{
  "meterReading": -50  // Negative - stored as-is (but invalid semantically)
}
```

---

## Error Handling

### Session Errors

```
401 Unauthorized
{
  "error": "Session expired due to inactivity"
}
```

**Action:** Login again to get new token

### Approval Errors

```
400 Bad Request
{
  "success": false,
  "message": "Only PENDING records can be approved by Manager"
}
```

**Action:** Check current status and correct workflow stage

```
403 Forbidden
{
  "success": false,
  "message": "Only MANAGER can perform this approval"
}
```

**Action:** Ensure user has required role

### Refill Errors

```
400 Bad Request
{
  "success": false,
  "message": "Liters must be a positive number"
}
```

**Action:** Provide positive liter amount

```
400 Bad Request
{
  "success": false,
  "message": "Exceeds tank capacity. You can add at most 150L"
}
```

**Action:** Reduce refill amount or check tank capacity

---

## Best Practices

### Session Management
- ✅ Always extract `X-New-Token` from response headers
- ✅ Update stored token immediately after each request
- ✅ Handle 401 + "inactivity" by redirecting to login
- ❌ Don't wait until token expires - refresh on every request

### Meter Reading Recording
- ✅ Record meter readings consistently for each operation
- ✅ Use same gauge/method for consistency
- ✅ Record gauge BEFORE refilling (not after)
- ✅ Leave meterReading empty if gauge not available
- ❌ Don't force meter readings if no gauge available

### Error Handling
- ✅ Check response status code first
- ✅ Handle both success and error responses
- ✅ Log failures for debugging
- ❌ Don't assume request succeeded without checking

---

## Testing

### Quick Test: Session Timeout

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass"}' | jq -r '.token')

# 2. Use token immediately
curl http://localhost:8080/api/fuel/records \
  -H "Authorization: Bearer $TOKEN"

# 3. Wait 30 minutes
sleep 1800

# 4. Try same token (should fail)
curl http://localhost:8080/api/fuel/records \
  -H "Authorization: Bearer $TOKEN"
# Response: 401 Unauthorized
```

### Quick Test: Meter Recording

```bash
# Refill with meter reading
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"liters": 500.00, "meterReading": 350.25}'

# Approve with meter reading
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "MANAGER_APPROVED", "meterReading": 450.00}'
```

---

## Support

For detailed information on each feature, see:
- **Session Timeout:** `SESSION_TIMEOUT.md`
- **Approval Meter Reading:** `METER_READING_FEATURE.md`
- **Tank Refill Meter Reading:** `TANK_REFILL_METER_READING.md`
- **Implementation Overview:** `IMPLEMENTATION_SUMMARY.md`
