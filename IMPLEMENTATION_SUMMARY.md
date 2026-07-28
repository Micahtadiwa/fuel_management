# Implementation Summary: Session Timeout & Meter Reading Features

## Overview

Two significant features have been implemented in the fuel management system:

1. **Activity-Based Session Timeout** (30 minutes of inactivity)
2. **Optional Meter Reading During Fuel Authorization**

## Feature 1: Activity-Based Session Timeout

### What Changed

#### Configuration
- **File:** `src/main/resources/application.properties`
- **New Property:** `jwt.inactivity-timeout=1800000` (30 minutes)

#### Code Changes

**1. JwtUtil.java** - Session Activity Tracking
- Added `inactivityTimeout` parameter injection
- Added `lastActivity` claim to JWT tokens
- Added `refreshToken()` method to update activity timestamp
- Added `isTokenInactive()` method to check if session expired

**2. JwtFilter.java** - Request Validation
- Check if token is inactive before processing request
- Return 401 status with "inactivity" message if expired
- Automatically refresh token on each valid request
- Send refreshed token in `X-New-Token` response header

**3. UserController.java** - Refresh Endpoint
- Added `POST /api/auth/refresh` endpoint
- Allows clients to explicitly refresh tokens
- Validates token status before refresh

#### Session Timeout Behavior

```
User logs in
    ↓
Gets JWT with lastActivity = current_time
    ↓
User is inactive for 30 minutes
    ↓
Next API request sent
    ↓
Server checks: (current_time - lastActivity) > 30 min?
    ↓
YES → 401 Unauthorized "Session expired due to inactivity"
NO → Request succeeds, token refreshed with new lastActivity
    ↓
Client gets response with X-New-Token header
    ↓
Client updates stored token for next request
```

#### Documentation
- **File:** `SESSION_TIMEOUT.md`
- Comprehensive guide for frontend integration
- JavaScript, React, and Angular examples
- API endpoint documentation
- Testing instructions
- Troubleshooting guide

### Key Implementation Details

- **Stateless**: No server-side session storage needed
- **Sliding Window**: Inactivity timer resets with each request
- **JWT-Based**: Activity timestamp stored in token claims
- **Backward Compatible**: No changes to existing client code required (but token refresh recommended)

### Frontend Integration Required

Clients must handle:
1. Extract `X-New-Token` header from responses
2. Store new token and use for next request
3. Handle 401 + "inactivity" response by redirecting to login

---

## Feature 2: Optional Meter Reading During Fuel Authorization

### What Changed

#### Data Model
- **File:** `src/main/java/com/tadiwa/fuel_management/Entity/FuelApprovalLog.java`
- Added `meterReading` field (DECIMAL 10,2)

#### API Request DTO
- **File:** `src/main/java/com/tadiwa/fuel_management/DTO/FuelStatusRequest.java`
- Added `meterReading` field (optional BigDecimal)

#### Service Logic
- **File:** `src/main/java/com/tadiwa/fuel_management/Service/FuelService.java`
- Overloaded `saveApprovalLog()` to accept meter reading
- Overloaded `updateFuelStatus()` to accept meter reading
- Updated `attendantApprove()` to handle meter reading
- All approval stages now support optional meter readings

#### Controller
- **File:** `src/main/java/com/tadiwa/fuel_management/Controller/FuelController.java`
- Updated `updateFuelStatus()` endpoint to pass meter reading from request

#### Database Migration
- **File:** `src/main/resources/db-migrations/V1_1_Add_Meter_Reading.sql`
- SQL script to add `meter_reading` column to `fuel_approval_log` table

#### Documentation
- **File:** `METER_READING_FEATURE.md`
- Complete guide for meter reading feature
- API examples for each approval stage
- Frontend implementation examples
- Database queries and reporting

### Meter Reading Workflow

```
Approval Stage 1: Manager
├─ Can optionally include meter_reading
├─ Status: MANAGER_APPROVED
└─ Example: meterReading: 450.00

Approval Stage 2: Attendant (Fuel Dispensed)
├─ Can optionally include meter_reading
├─ Status: ATTENDANT_APPROVED
└─ Example: meterReading: 445.50

Approval Stage 3: Finance (Sign-off)
├─ Can optionally include meter_reading
├─ Status: SIGNED
└─ Example: meterReading: 445.00
```

### API Endpoint Examples

#### With Meter Reading
```bash
PATCH /api/fuel/123/status
{
  "status": "MANAGER_APPROVED",
  "meterReading": 450.25
}
```

#### Without Meter Reading (Optional)
```bash
PATCH /api/fuel/123/status
{
  "status": "MANAGER_APPROVED"
}
```

### Key Features

- **Optional**: Meter readings are never required
- **Flexible**: Can be provided at any approval stage
- **Audit Trail**: All recordings stored with approval logs
- **Backward Compatible**: Existing workflows unaffected
- **Flexible Precision**: Stored as DECIMAL(10,2) - supports up to 9999.99 liters

---

## Database Migration

### Apply Migration

Run the migration script to add meter_reading column:

```bash
mysql -u root -p transport < src/main/resources/db-migrations/V1_1_Add_Meter_Reading.sql
```

Or manually execute:

```sql
ALTER TABLE fuel_approval_log
ADD COLUMN meter_reading DECIMAL(10, 2) NULL
COMMENT 'Optional meter reading (e.g., fuel tank gauge) recorded by authorizer during approval';
```

---

## Compilation & Testing

### Build Project
```bash
cd fuel_management
./mvnw clean compile
```

### Verify Compilation
- No errors expected
- All new classes properly integrated
- Backward compatible with existing code

---

## Files Modified/Created

### Modified Files
1. `src/main/resources/application.properties` - Added jwt.inactivity-timeout
2. `src/main/java/com/tadiwa/fuel_management/Security/JwtUtil.java` - Activity tracking
3. `src/main/java/com/tadiwa/fuel_management/Security/JwtFilter.java` - Inactivity validation
4. `src/main/java/com/tadiwa/fuel_management/DTO/FuelStatusRequest.java` - Added meterReading
5. `src/main/java/com/tadiwa/fuel_management/Entity/FuelApprovalLog.java` - Added meterReading column
6. `src/main/java/com/tadiwa/fuel_management/Service/FuelService.java` - Meter reading handling
7. `src/main/java/com/tadiwa/fuel_management/Controller/UserController.java` - Added refresh endpoint
8. `src/main/java/com/tadiwa/fuel_management/Controller/FuelController.java` - Pass meter reading

### New Files
1. `SESSION_TIMEOUT.md` - Session timeout documentation
2. `METER_READING_FEATURE.md` - Meter reading documentation
3. `IMPLEMENTATION_SUMMARY.md` - This file
4. `src/main/resources/db-migrations/V1_1_Add_Meter_Reading.sql` - Database migration

---

## Integration Checklist

### Frontend Developer TODO

#### Session Timeout
- [ ] Implement token refresh interceptor/middleware
- [ ] Extract `X-New-Token` header from API responses
- [ ] Update stored token with refreshed token
- [ ] Handle 401 + "inactivity" error by redirecting to login
- [ ] Optionally show warning before timeout (e.g., at 25 minutes)
- [ ] Reference: `SESSION_TIMEOUT.md` for examples

#### Meter Reading Feature
- [ ] Add meter reading input field to fuel approval forms
- [ ] Make input optional (not required)
- [ ] Include meterReading in approval request body
- [ ] Display meter readings in approval history/reports
- [ ] Optionally add validation (e.g., range 0-9999.99)
- [ ] Reference: `METER_READING_FEATURE.md` for API details

### Backend/DevOps TODO

#### Session Timeout
- [ ] Verify `jwt.inactivity-timeout` configuration
- [ ] Test token refresh on multiple requests
- [ ] Test session expiration after 30 minutes inactivity
- [ ] Verify backward compatibility with existing clients

#### Meter Reading Feature
- [ ] Apply database migration
- [ ] Verify `meter_reading` column added to `fuel_approval_log`
- [ ] Test approvals with meter readings
- [ ] Test approvals without meter readings (backward compatibility)
- [ ] Verify data storage in approval logs
- [ ] Test approval history retrieval

---

## Configuration Reference

### Session Timeout Configuration

Edit `src/main/resources/application.properties`:

```properties
jwt.inactivity-timeout=1800000  # 30 minutes in milliseconds

# Other options:
# 5 minutes:   300000
# 10 minutes:  600000
# 30 minutes:  1800000  (default)
# 1 hour:      3600000
```

### Token Expiration (separate from inactivity)

```properties
jwt.expiration=86400000  # 24 hours in milliseconds
```

**Note:** A token can expire in two ways:
1. **Absolute Expiration**: After 24 hours (jwt.expiration)
2. **Inactivity Expiration**: After 30 minutes without activity (jwt.inactivity-timeout)

---

## Examples

### Session Timeout Flow

**Request 1 (t=0 min)**
```
Request: GET /api/fuel/records
Headers: Authorization: Bearer eyJhbGc...

Response: 200 OK
Headers: X-New-Token: eyJhbGc...NEW
Body: { records: [...] }
```

**Request 2 (t=15 min)**
```
Request: GET /api/fuel/tank/1
Headers: Authorization: Bearer eyJhbGc...NEW

Response: 200 OK
Headers: X-New-Token: eyJhbGc...NEW2
Body: { tank: { level: 450 } }
```

**Request 3 (t=45 min - No activity for 30+ minutes)**
```
Request: GET /api/fuel/records
Headers: Authorization: Bearer eyJhbGc...OLD

Response: 401 Unauthorized
Body: { "error": "Session expired due to inactivity" }
```

### Meter Reading Flow

**Approval 1: Manager Approves**
```
PATCH /api/fuel/123/status
{
  "status": "MANAGER_APPROVED",
  "meterReading": 450.00
}

Response: { "success": true, "message": "Record approved by Manager..." }
```

**Approval 2: Attendant Dispenses**
```
PATCH /api/fuel/123/status
{
  "status": "ATTENDANT_APPROVED",
  "meterReading": 445.50
}

Response: { 
  "success": true, 
  "message": "Approved by Attendant — 50.00L petrol dispensed from tank"
}
```

**Approval 3: Finance Signs**
```
PATCH /api/fuel/123/status
{
  "status": "SIGNED",
  "meterReading": 445.00
}

Response: { "success": true, "message": "Record signed by Finance" }
```

---

## Verification

### Test Session Timeout

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Response: { "token": "eyJhbGc...", ... }

# 2. Use token immediately (should work)
curl http://localhost:8080/api/fuel/records \
  -H "Authorization: Bearer eyJhbGc..."

# Response: 200 OK

# 3. Wait 30 minutes
sleep 1800

# 4. Use same token (should fail)
curl http://localhost:8080/api/fuel/records \
  -H "Authorization: Bearer eyJhbGc..."

# Response: 401 Unauthorized
# { "error": "Session expired due to inactivity" }

# 5. Refresh token using refresh endpoint
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer eyJhbGc..."

# Response: { "token": "eyJhbGc...NEW", "message": "..." }
```

### Test Meter Reading

```bash
# Approve with meter reading
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"status":"MANAGER_APPROVED","meterReading":450.25}'

# Query approval log
SELECT * FROM fuel_approval_log WHERE fuel_record_id = 123;

# Should show meter_reading = 450.25
```

---

## Support & Documentation

### Key Documentation Files

1. **SESSION_TIMEOUT.md**
   - Complete session timeout implementation guide
   - Frontend integration examples (JS, React, Angular)
   - API documentation
   - Testing procedures

2. **METER_READING_FEATURE.md**
   - Meter reading feature guide
   - API examples for each approval stage
   - Database queries and reporting
   - Best practices and troubleshooting

3. **IMPLEMENTATION_SUMMARY.md** (this file)
   - High-level overview of both features
   - Files modified/created
   - Integration checklist
   - Configuration reference

### Questions?

Refer to the appropriate documentation:
- Session timeout issues → **SESSION_TIMEOUT.md**
- Meter reading issues → **METER_READING_FEATURE.md**
- Implementation overview → **IMPLEMENTATION_SUMMARY.md**
