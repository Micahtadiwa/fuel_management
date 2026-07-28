# Tank Refill with Optional Meter Reading

## Overview

The fuel management system now allows operators to optionally record meter readings (tank gauge levels) when refilling tanks. This feature helps track actual fuel levels at the time of refill operations.

## Feature Details

### What is a Meter Reading During Refill?

A meter reading during refill is the recorded measurement of fuel in the tank (in liters) at the time the refill is performed. For example:
- Before refill: Tank shows 350 liters on gauge
- Operator records this meter reading when refilling
- Helps verify tank capacity and detect discrepancies

### Who Can Record Meter Readings?

Any user with tank refill permissions (typically ADMIN_MAKER or MANAGER) can optionally include a meter reading:
- **ADMIN_MAKER** - Can refill tanks and optionally record meter readings
- **MANAGER** - Can refill tanks and optionally record meter readings (if permitted)

### Workflow

```
Tank Refill Operation
    ↓
Operator enters liters to refill
    ↓
Operator optionally records tank meter reading (gauge level)
    ↓
System stores:
  - Liters added
  - New tank level (calculated)
  - Meter reading (if provided)
  - Transaction timestamp
    ↓
Fuel transaction log updated with all details
```

## API Integration

### Refill Tank WITH Meter Reading

**Endpoint:** `POST /api/fuel/tank/{id}/refill`

**Request Body - With Meter Reading:**
```json
{
  "liters": 500.00,
  "meterReading": 350.25
}
```

**Request Body - Without Meter Reading (optional):**
```json
{
  "liters": 500.00
}
```

**Response:**
```json
{
  "success": true,
  "message": "Tank refilled successfully",
  "current_level": 850.25,
  "capacity": 1000,
  "percent_full": 85
}
```

### Examples

#### Refill Petrol Tank with Meter Reading
```bash
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 500.00,
    "meterReading": 350.25
  }'
```

#### Refill Diesel Tank with Meter Reading
```bash
curl -X POST http://localhost:8080/api/fuel/tank/2/refill \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 300.00,
    "meterReading": 200.50
  }'
```

#### Refill Without Recording Meter Reading
```bash
curl -X POST http://localhost:8080/api/fuel/tank/1/refill \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 500.00
  }'
```

## Data Storage

### Fuel Transactions Table Schema

The `fuel_transactions` table has been updated with a new column:

```sql
ALTER TABLE fuel_transactions
ADD COLUMN meter_reading DECIMAL(10, 2) NULL;
```

### Transaction Record Example

```json
{
  "id": 42,
  "tank_id": 1,
  "movement_type": "IN",
  "litres": 500.00,
  "balance_after": 850.25,
  "source": "REFILL",
  "reference": "Tank refill 1",
  "created_by": 5,
  "created_at": "2026-07-07T14:30:00",
  "meter_reading": 350.25
}
```

## Frontend Implementation

### JavaScript/TypeScript Example

```typescript
interface RefillRequest {
  liters: number;
  meterReading?: number;  // Optional
}

async function refillTankWithMeter(
  tankId: number,
  liters: number,
  meterReading?: number
): Promise<void> {
  const request: RefillRequest = { liters };
  
  if (meterReading !== undefined && meterReading !== null) {
    request.meterReading = meterReading;
  }

  const response = await fetch(`/api/fuel/tank/${tankId}/refill`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(`Refill failed: ${response.statusText}`);
  }

  return response.json();
}

// Usage
await refillTankWithMeter(1, 500.00, 350.25);  // With meter reading
await refillTankWithMeter(1, 500.00);           // Without meter reading
```

### React Example

```typescript
import React, { useState } from 'react';

interface RefillFormProps {
  tankId: number;
  tankName: string;
  tankCapacity: number;
}

export function TankRefillForm({ tankId, tankName, tankCapacity }: RefillFormProps) {
  const [liters, setLiters] = useState<number | null>(null);
  const [meterReading, setMeterReading] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleRefill = async () => {
    if (!liters || liters <= 0) {
      setError('Liters must be a positive number');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await fetch(`/api/fuel/tank/${tankId}/refill`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          liters,
          meterReading: meterReading || undefined
        })
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.message || 'Failed to refill tank');
      }

      const data = await response.json();
      setSuccess(`Tank refilled successfully! New level: ${data.current_level}L (${data.percent_full}%)`);
      
      // Reset form
      setLiters(null);
      setMeterReading(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="refill-form">
      <h3>Refill {tankName}</h3>
      <p>Tank Capacity: {tankCapacity}L</p>
      
      <div className="form-group">
        <label htmlFor="liters">Liters to Add*</label>
        <input
          id="liters"
          type="number"
          step="0.01"
          min="0.01"
          value={liters ?? ''}
          onChange={(e) => setLiters(
            e.target.value ? parseFloat(e.target.value) : null
          )}
          placeholder="Enter liters to refill"
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="meter-reading">
          Meter Reading (Tank Gauge Level) - Optional
        </label>
        <input
          id="meter-reading"
          type="number"
          step="0.01"
          min="0"
          value={meterReading ?? ''}
          onChange={(e) => setMeterReading(
            e.target.value ? parseFloat(e.target.value) : null
          )}
          placeholder="Enter current tank level from gauge"
        />
        <small>Optional: Record the gauge reading before refilling</small>
      </div>

      {error && <div className="error">{error}</div>}
      {success && <div className="success">{success}</div>}

      <button 
        onClick={handleRefill}
        disabled={isSubmitting || !liters}
      >
        {isSubmitting ? 'Refilling...' : 'Refill Tank'}
      </button>
    </div>
  );
}
```

### Angular Example

```typescript
import { Component, Input } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface RefillRequest {
  liters: number;
  meterReading?: number;
}

interface RefillResponse {
  success: boolean;
  message: string;
  current_level: number;
  capacity: number;
  percent_full: number;
}

@Component({
  selector: 'app-tank-refill',
  template: `
    <form (ngSubmit)="onRefill()">
      <h3>{{ tankName }}</h3>
      <p>Tank Capacity: {{ tankCapacity }}L</p>
      
      <div class="form-group">
        <label>Liters to Add*</label>
        <input 
          type="number" 
          step="0.01"
          min="0.01"
          [(ngModel)]="liters" 
          name="liters"
          required
          placeholder="Enter liters"
        />
      </div>

      <div class="form-group">
        <label>Meter Reading (Tank Gauge Level) - Optional</label>
        <input 
          type="number" 
          step="0.01"
          min="0"
          [(ngModel)]="meterReading" 
          name="meterReading"
          placeholder="Enter tank gauge reading"
        />
        <small>Optional: Record the gauge reading before refilling</small>
      </div>

      <div *ngIf="error" class="error">{{ error }}</div>
      <div *ngIf="success" class="success">{{ success }}</div>

      <button type="submit" [disabled]="isSubmitting || !liters">
        {{ isSubmitting ? 'Refilling...' : 'Refill Tank' }}
      </button>
    </form>
  `
})
export class TankRefillComponent {
  @Input() tankId!: number;
  @Input() tankName!: string;
  @Input() tankCapacity!: number;
  
  liters: number | null = null;
  meterReading: number | null = null;
  isSubmitting = false;
  error: string | null = null;
  success: string | null = null;

  constructor(private http: HttpClient) {}

  onRefill(): void {
    if (!this.liters || this.liters <= 0) {
      this.error = 'Liters must be a positive number';
      return;
    }

    this.isSubmitting = true;
    this.error = null;
    this.success = null;

    const request: RefillRequest = { liters: this.liters };
    if (this.meterReading !== null) {
      request.meterReading = this.meterReading;
    }

    this.http.post<RefillResponse>(`/api/fuel/tank/${this.tankId}/refill`, request)
      .subscribe({
        next: (response) => {
          this.success = `Tank refilled successfully! New level: ${response.current_level}L (${response.percent_full}%)`;
          this.liters = null;
          this.meterReading = null;
        },
        error: (err) => {
          this.error = err.error?.message || 'Failed to refill tank';
        },
        complete: () => {
          this.isSubmitting = false;
        }
      });
  }
}
```

## Database Migration

### Apply Migration

Run the migration script to add meter_reading columns:

```bash
mysql -u root -p transport < src/main/resources/db-migrations/V1_1_Add_Meter_Reading.sql
```

Or manually execute:

```sql
ALTER TABLE fuel_transactions
ADD COLUMN meter_reading DECIMAL(10, 2) NULL
COMMENT 'Optional meter reading recorded during fuel transaction';
```

## Querying Refill Records with Meter Readings

### Get All Refill Transactions with Meter Readings

```sql
SELECT 
  id,
  tank_id,
  movement_type,
  litres,
  balance_after,
  source,
  created_at,
  meter_reading,
  CASE 
    WHEN meter_reading IS NOT NULL 
    THEN CONCAT(meter_reading, 'L')
    ELSE 'Not recorded'
  END as gauge_reading
FROM fuel_transactions
WHERE movement_type = 'IN' 
  AND meter_reading IS NOT NULL
ORDER BY created_at DESC;
```

### Get Refill History for Specific Tank

```sql
SELECT 
  id,
  litres,
  balance_after,
  meter_reading,
  created_at,
  CASE 
    WHEN meter_reading IS NOT NULL 
    THEN ROUND(((balance_after - litres) - meter_reading), 2)
    ELSE NULL
  END as discrepancy
FROM fuel_transactions
WHERE tank_id = 1 
  AND movement_type = 'IN'
ORDER BY created_at DESC;
```

## Validation

### Meter Reading Constraints
- **Format:** Decimal number with up to 2 decimal places
- **Range:** 0 to 9999.99 (liters)
- **Required:** No (optional field)
- **Precision:** 10 digits total, 2 decimal places

### API Validation

The API automatically validates meter readings:
```json
{
  "liters": 500.00,
  "meterReading": 350.25  // Valid
}
```

Invalid meter readings are silently ignored:
```json
{
  "liters": 500.00,
  "meterReading": "not a number"  // Will be ignored
}
```

## Reporting

### Refill Activity Report with Meter Readings

```sql
SELECT 
  ft.id as transaction_id,
  ft.tank_id,
  CASE ft.tank_id 
    WHEN 1 THEN 'Petrol Tank'
    WHEN 2 THEN 'Diesel Tank'
    ELSE 'Unknown'
  END as tank_name,
  ft.litres,
  ft.balance_after as level_after_refill,
  ft.meter_reading as gauge_reading_before_refill,
  ft.created_at,
  u.username as refilled_by
FROM fuel_transactions ft
LEFT JOIN users u ON ft.created_by = u.user_id
WHERE ft.movement_type = 'IN'
  AND ft.source = 'REFILL'
ORDER BY ft.created_at DESC;
```

### Tank Level Variance Report

Compare system calculated levels with gauge readings:

```sql
SELECT 
  ft.tank_id,
  ft.created_at,
  ft.litres as refill_amount,
  ft.balance_after as system_level,
  ft.meter_reading as gauge_reading,
  CASE 
    WHEN ft.meter_reading IS NOT NULL
    THEN ft.meter_reading + ft.litres - ft.balance_after
    ELSE NULL
  END as variance
FROM fuel_transactions ft
WHERE ft.movement_type = 'IN'
  AND ft.meter_reading IS NOT NULL
ORDER BY ft.created_at DESC;
```

## Best Practices

1. **Consistent Recording**: 
   - Record meter readings every time you refill
   - Use the same gauge/method for consistency
   - Note any gauge calibration issues

2. **Accuracy**: 
   - Record meter readings before refilling
   - Use precise readings from gauges
   - Round to nearest 0.01 liter

3. **Audit Purpose**: Meter readings help:
   - Verify fuel tank capacity over time
   - Detect fuel leaks or evaporation
   - Track fuel consumption accuracy
   - Reconcile inventory discrepancies

4. **System Integration**:
   - Display variance between gauge readings and system calculations
   - Alert on unusual tank level changes
   - Track gauge accuracy for maintenance

## Troubleshooting

### Meter Reading Not Saved
- Verify database migration has been applied
- Check that `meter_reading` column exists in `fuel_transactions`
- Ensure the API request includes meterReading in the JSON body

### Authorization Issues
- Only authorized roles can refill tanks (ADMIN_MAKER, MANAGER)
- No additional permissions required for meter reading
- Check user role and permissions

### Data Type Issues
- Meter readings must be numeric (decimal/float)
- Avoid sending as strings: use `350.25` not `"350.25"`
- Maximum precision: 10 digits with 2 decimal places

### Refill Capacity Error
- Ensure total fuel (current + refill amount) doesn't exceed tank capacity
- Example: 500L tank with 450L current → can only add 550L more
- Check tank capacity configuration

## Implementation Notes

### Files Modified
- `src/main/java/com/tadiwa/fuel_management/DTO/RefillRequest.java` - Added meterReading field
- `src/main/java/com/tadiwa/fuel_management/DTO/DispenseRequest.java` - Added meterReading field
- `src/main/java/com/tadiwa/fuel_management/Entity/FuelTransaction.java` - Added meter_reading column
- `src/main/java/com/tadiwa/fuel_management/Service/FuelService.java` - Updated logFuelTransaction and refillTank methods
- `src/main/resources/db-migrations/V1_1_Add_Meter_Reading.sql` - Database migration

### Backward Compatibility

This feature is fully backward compatible:
- The meterReading field is optional in all API requests
- Existing refill workflows continue to work without meter readings
- Database columns are nullable, existing records remain unchanged

### Transaction Recording

All fuel transactions now support meter readings:
- **Refills** (IN) - Record gauge level before refilling
- **Dispensing** (OUT) - Record tank level when fuel is dispensed
- **Approvals** - Meter readings stored in approval log (separate feature)

Each transaction type maintains its own meter reading for complete audit trail.
