# Tank Level and Meters Field Alignment

This document outlines the changes made to align the database, backend, and frontend for tracking meters and tank level during fuel transactions.

## Overview

The system now supports capturing two optional fields during fuel operations:
- **Meter Reading**: Current meter/gauge reading on the fuel tank (numeric decimal value)
- **Tank Level**: Tank level when refilling/dispensing (numeric decimal value)

These fields are optional and can be recorded by attendants during fuel transactions.

## Database Changes

### New Migrations

1. **V1_2_Add_Tank_Level.sql**: Adds `tank_level` column to `fuel_transactions` table
2. **V1_3_Add_Tank_Level_To_Approval_Log.sql**: Adds `tank_level` column to `fuel_approval_log` table

### Schema Updates

#### fuel_transactions table
```sql
ALTER TABLE fuel_transactions
ADD COLUMN tank_level DECIMAL(10, 2) NULL
COMMENT 'Optional tank level recorded during fuel transaction';
```

#### fuel_approval_log table
```sql
ALTER TABLE fuel_approval_log
ADD COLUMN tank_level DECIMAL(10, 2) NULL
COMMENT 'Optional tank level recorded by authorizer during approval';
```

## Backend Changes

### Entity Updates

#### FuelTransaction.java
- Added `tankLevel` field with BigDecimal type
- Added getter/setter methods

#### FuelApprovalLog.java
- Added `tankLevel` field with BigDecimal type
- Added getter/setter methods

### DTO Updates

#### RefillRequest.java
- Added `tankLevel` field (BigDecimal)
- Added getter/setter methods

#### DispenseRequest.java
- Added `tankLevel` field (BigDecimal)
- Added getter/setter methods

#### FuelStatusRequest.java
- Added `tankLevel` field (BigDecimal)
- Added getter/setter methods

### Service Changes (FuelService.java)

#### Modified Methods

1. **logFuelTransaction()**: 
   - Updated to accept `tankLevel` parameter
   - Overloaded versions for backward compatibility

2. **saveApprovalLog()**: 
   - Updated to accept `tankLevel` parameter
   - Overloaded versions for backward compatibility

3. **updateFuelStatus()**:
   - Updated to accept and pass `tankLevel`
   - Overloaded versions for backward compatibility

4. **attendantApprove()**:
   - Updated to accept and pass `tankLevel`
   - Logs meter reading and tank level in fuel transaction

5. **dispenseFuel()**:
   - Now passes `tankLevel` from request to `logFuelTransaction()`

6. **refillTank()**:
   - Now passes `tankLevel` from request to `logFuelTransaction()`

7. **getTankRefills()**:
   - Added `meter_reading` and `tank_level` to SELECT clause
   - Returns meter readings and tank levels to frontend

8. **getApprovalLog()**:
   - Added `meter_reading` and `tank_level` to SELECT clause
   - Returns meter readings and tank levels to frontend

### Controller Changes (FuelController.java)

#### updateFuelStatus()
- Updated to pass `tankLevel` from `FuelStatusRequest` to service method

## API Request/Response Examples

### Refill Tank Request
```json
POST /api/fuel/tank/{id}/refill
{
  "liters": 500,
  "meterReading": 45000.50,
  "tankLevel": 95.5
}
```

### Dispense Fuel Request
```json
POST /api/fuel/dispense
{
  "liters": 50,
  "fuel_type": "petrol",
  "meterReading": 45012.75,
  "tankLevel": 92.0
}
```

### Update Fuel Status Request
```json
PATCH /api/fuel/{id}/status
{
  "status": "MANAGER_APPROVED",
  "meterReading": 45000.00,
  "tankLevel": 93.5
}
```

### Tank Refills Response
```json
GET /api/fuel/tank-refills
[
  {
    "id": 1,
    "tank_id": 1,
    "fuelType": "petrol",
    "litres": 500,
    "balanceAfter": 550,
    "reference": "Tank refill 1",
    "createdAt": "2026-07-07T10:30:00",
    "refilledBy": "admin",
    "meterReading": 45000.50,
    "tankLevel": 95.5
  }
]
```

### Approval Log Response
```json
GET /api/fuel/approval-log
[
  {
    "id": 1,
    "fuel_record_id": 1,
    "action": "MANAGER_APPROVED",
    "actioned_by_username": "manager",
    "position": "MANAGER",
    "actioned_at": "2026-07-07T10:30:00",
    "vehicle": "ABC123",
    "fuel_type": "petrol",
    "liters": 50,
    "fuel_date": "2026-07-07",
    "meterReading": 45000.00,
    "tankLevel": 93.5
  }
]
```

## Frontend Integration

### Form Fields to Add

For any fuel transaction form (Refill, Dispense, Approval), add:

1. **Meter Reading Input**
   - Type: Number (decimal)
   - Label: "Meter Reading" or "Tank Meter"
   - Placeholder: "e.g., 45000.50"
   - Optional: true
   - Help text: "Current meter/gauge reading on the fuel tank"

2. **Tank Level Input**
   - Type: Number (decimal)
   - Label: "Tank Level" or "Tank Level (%)"
   - Placeholder: "e.g., 95.5"
   - Optional: true
   - Help text: "Tank level percentage or gauge reading"

### Display Fields

For any fuel transaction history/report (Tank Refills, Approval Log), display:

1. **meterReading**: Show as numeric value with unit context
2. **tankLevel**: Show as numeric value with % or gauge context

### Example React Component (Refill Form)

```jsx
<form>
  <input 
    type="number" 
    name="liters" 
    placeholder="Liters" 
    required 
  />
  
  <input 
    type="number" 
    name="meterReading" 
    placeholder="Meter Reading" 
    step="0.01" 
  />
  
  <input 
    type="number" 
    name="tankLevel" 
    placeholder="Tank Level" 
    step="0.1" 
  />
  
  <button type="submit">Refill</button>
</form>
```

### Example Table Display

```jsx
<table>
  <thead>
    <tr>
      <th>Refilled By</th>
      <th>Liters</th>
      <th>Balance</th>
      <th>Meter Reading</th>
      <th>Tank Level</th>
      <th>Date</th>
    </tr>
  </thead>
  <tbody>
    {refills.map(refill => (
      <tr key={refill.id}>
        <td>{refill.refilledBy}</td>
        <td>{refill.litres}L</td>
        <td>{refill.balanceAfter}L</td>
        <td>{refill.meterReading}</td>
        <td>{refill.tankLevel}%</td>
        <td>{new Date(refill.createdAt).toLocaleDateString()}</td>
      </tr>
    ))}
  </tbody>
</table>
```

## Running the Application

1. **Build the application** (migrations will run automatically):
   ```bash
   mvn clean install
   ```

2. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Database migrations** will execute automatically on startup

## Testing

### Test the Refill Endpoint
```bash
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 500,
    "meterReading": 45000.50,
    "tankLevel": 95.5
  }'
```

### Test the Dispense Endpoint
```bash
curl -X POST http://localhost:8080/api/fuel/dispense \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 50,
    "fuel_type": "petrol",
    "meterReading": 45012.75,
    "tankLevel": 92.0
  }'
```

### Test the Status Update Endpoint
```bash
curl -X PATCH http://localhost:8080/api/fuel/1/status \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "MANAGER_APPROVED",
    "meterReading": 45000.00,
    "tankLevel": 93.5
  }'
```

## Backward Compatibility

All changes maintain backward compatibility:
- Fields are optional in requests (nullable in database)
- Existing endpoints continue to work without providing meter_reading or tank_level
- Overloaded method signatures allow existing code to continue working

## Summary

All three layers (database, backend, frontend) are now aligned to support meters and tank level tracking:

✅ **Database**: New columns added for tank_level  
✅ **Backend**: DTOs, Entities, and Service methods updated  
✅ **Backend API**: New fields returned in responses  
✅ **Frontend**: Ready to display meter_reading and tankLevel from API responses

The frontend developer needs to:
1. Add input fields for meters and tank level in forms
2. Display these fields in transaction history tables
3. Send these values in API requests (optional)
4. Display the returned values in responses
