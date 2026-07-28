# Refill Verification Guide

## How Refill Works

1. **Get Current Level**: Queries the latest fuel_transaction for the tank, gets `balance_after`
2. **Calculate New Level**: `newLevel = currentLevel + litersToAdd`
3. **Validate**: Check if `newLevel <= capacity`
4. **Log Transaction**: Create a FuelTransaction with:
   - `movementType` = "IN"
   - `litres` = amount added
   - `balanceAfter` = new level
   - `source` = "REFILL"
5. **Return Response**: Current level and percentage full

## Step 1: Start the Application

```bash
mvn spring-boot:run
```

Wait for startup messages including:
```
✓ Added tank_level column to fuel_transactions
✓ Added tank_level column to fuel_approval_log
Application started
```

## Step 2: Get Authentication Token

First, login to get a JWT token:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }' | jq '.token'
```

Save the token as `TOKEN="your_token_here"`

## Step 3: Check Current Tank Level

```bash
curl -X GET http://localhost:8080/api/fuel/tank/1 \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

Expected response:
```json
{
  "success": true,
  "id": 1,
  "capacity": 1000,
  "current_level": 150.5,
  "percent_full": 15
}
```

## Step 4: Refill the Tank

```bash
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 500,
    "meterReading": 45000.50,
    "tankLevel": 95.5
  }' | jq '.'
```

Expected response:
```json
{
  "success": true,
  "message": "Tank refilled successfully",
  "current_level": 650.5,
  "capacity": 1000,
  "percent_full": 65
}
```

**The fuel level should INCREASE from 150.5 to 650.5 liters.**

## Step 5: Verify Tank Level Increased

```bash
curl -X GET http://localhost:8080/api/fuel/tank/1 \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

Should now show:
```json
{
  "success": true,
  "id": 1,
  "capacity": 1000,
  "current_level": 650.5,
  "percent_full": 65
}
```

## Step 6: Check Refill History

```bash
curl -X GET http://localhost:8080/api/fuel/tank-refills \
  -H "Authorization: Bearer $TOKEN" | jq '.[-1]'
```

Should show your recent refill:
```json
{
  "id": 123,
  "tank_id": 1,
  "fuelType": "petrol",
  "litres": 500,
  "balanceAfter": 650.5,
  "reference": "Tank refill 1",
  "createdAt": "2026-07-07T14:30:00",
  "refilledBy": "admin",
  "meterReading": 45000.50,
  "tankLevel": 95.5
}
```

## Troubleshooting

### Issue: "Exceeds tank capacity"
**Cause**: The new level would exceed the tank capacity
**Fix**: Use fewer liters. Max allowed = `capacity - currentLevel`

```bash
# Tank capacity is 1000, current is 650.5
# Maximum you can add = 1000 - 650.5 = 349.5 liters
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"liters": 349.5}' | jq '.'
```

### Issue: "Tank not found"
**Cause**: Tank ID doesn't exist
**Fix**: Use Tank ID 1 (petrol) or 2 (diesel)

### Issue: Fuel level not increasing
**Possible causes**:
1. Transaction not saved - check database
2. Using wrong tank ID - verify with `GET /api/fuel/tank/1`
3. Old data cached - restart the application

## Verify in Database

If you need to verify directly in MySQL:

```sql
-- Check current fuel level for tank 1
SELECT
  CASE tank_id WHEN 1 THEN 'Petrol' WHEN 2 THEN 'Diesel' END AS fuel_type,
  litres AS amount_added,
  balance_after AS current_level,
  movement_type,
  source,
  created_at
FROM fuel_transactions
WHERE tank_id = 1
ORDER BY created_at DESC
LIMIT 10;
```

Should show your refill with `movement_type = 'IN'` and the new `balance_after`.

## Summary

✅ Refill works by:
1. Reading latest fuel_transaction balance_after
2. Adding requested liters
3. Saving new transaction with updated balance_after
4. Subsequent queries read the new balance

The refill increments fuel in the tank with each call!
