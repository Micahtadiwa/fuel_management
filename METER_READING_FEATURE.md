# Meter Reading Feature for Fuel Authorization

## Overview

The fuel management system now allows authorizers (MANAGER, ATTENDANT, FINANCE) to optionally record meter readings when approving fuel requests. This feature helps track actual fuel tank levels at the time of authorization.

## Feature Details

### What is a Meter Reading?

A meter reading is the recorded measurement of fuel in the tank (in liters) at the time of authorization. For example:
- A MANAGER might record that the petrol tank had 450 liters when they approved a fuel request
- An ATTENDANT might record that the diesel tank had 220 liters when they authorized fuel dispensing
- A FINANCE officer might record the tank level when signing off on the transaction

### Who Can Record Meter Readings?

Any user authorized to approve fuel can optionally include a meter reading:
- **MANAGER** - Can include meter reading when approving (MANAGER_APPROVED status)
- **ATTENDANT** - Can include meter reading when approving fuel dispensing (ATTENDANT_APPROVED status)
- **FINANCE** - Can include meter reading when signing records (SIGNED status)
- **ADMIN_MAKER** - Can include meter readings at any approval stage

### Workflow

```
1. Driver submits fuel request → PENDING

2. Manager reviews and approves (optionally with meter reading)
   └─ Status: MANAGER_APPROVED
   └─ Optional: meter_reading = 450.00 (liters in tank)

3. Attendant authorizes fuel dispensing (optionally with meter reading)
   └─ Status: ATTENDANT_APPROVED
   └─ Optional: meter_reading = 445.50 (after dispensing)

4. Finance signs the record (optionally with meter reading)
   └─ Status: SIGNED
   └─ Optional: meter_reading = 445.00 (final tank level)
```

## API Integration

### Approving Fuel WITH Meter Reading

**Endpoint:** `PATCH /api/fuel/{id}/status`

**Request Body - With Meter Reading:**
```json
{
  "status": "MANAGER_APPROVED",
  "meterReading": 450.25
}
```

**Request Body - Without Meter Reading (optional):**
```json
{
  "status": "MANAGER_APPROVED"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Record approved by Manager — awaiting Attendant approval"
}
```

### Examples for Each Approval Stage

#### Manager Approval with Meter Reading
```bash
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "MANAGER_APPROVED",
    "meterReading": 450.00
  }'
```

#### Attendant Approval with Meter Reading
```bash
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ATTENDANT_APPROVED",
    "meterReading": 445.50
  }'
```

#### Finance Sign-off with Meter Reading
```bash
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SIGNED",
    "meterReading": 445.00
  }'
```

#### Decline with Optional Meter Reading
```bash
curl -X PATCH http://localhost:8080/api/fuel/123/status \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DECLINED",
    "meterReading": 450.00
  }'
```

## Data Storage

### Approval Log Table Schema

The `fuel_approval_log` table has been updated with a new column:

```sql
ALTER TABLE fuel_approval_log
ADD COLUMN meter_reading DECIMAL(10, 2) NULL;
```

### Approval Log Record Example

```json
{
  "id": 1,
  "fuel_record_id": 42,
  "action": "MANAGER_APPROVED",
  "actioned_by_user_id": 5,
  "actioned_by_username": "john_smith",
  "actioned_by_role": "MANAGER",
  "actioned_at": "2026-07-07T14:30:00",
  "vehicle": "ABC-1234",
  "fuel_type": "petrol",
  "liters": 50.00,
  "fuel_date": "2026-07-07",
  "meter_reading": 450.25
}
```

## Frontend Implementation

### JavaScript/TypeScript Example

```typescript
interface FuelApprovalRequest {
  status: string;
  meterReading?: number;  // Optional
}

async function approveFuelWithMeter(
  fuelRecordId: number,
  status: string,
  meterReading?: number
): Promise<void> {
  const request: FuelApprovalRequest = { status };
  
  if (meterReading !== undefined && meterReading !== null) {
    request.meterReading = meterReading;
  }

  const response = await fetch(`/api/fuel/${fuelRecordId}/status`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(`Approval failed: ${response.statusText}`);
  }

  return response.json();
}

// Usage
await approveFuelWithMeter(123, 'MANAGER_APPROVED', 450.25);
await approveFuelWithMeter(123, 'ATTENDANT_APPROVED');  // No meter reading
```

### React Example

```typescript
import React, { useState } from 'react';

interface ApprovalFormProps {
  fuelRecordId: number;
  currentStatus: string;
  nextStatus: string;
}

export function FuelApprovalForm({ fuelRecordId, currentStatus, nextStatus }: ApprovalFormProps) {
  const [meterReading, setMeterReading] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleApprove = async () => {
    setIsSubmitting(true);
    setError(null);

    try {
      const response = await fetch(`/api/fuel/${fuelRecordId}/status`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          status: nextStatus,
          meterReading: meterReading || undefined
        })
      });

      if (!response.ok) {
        throw new Error('Failed to approve fuel request');
      }

      // Handle success
      console.log('Fuel approved successfully');
      // Refresh data or navigate
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="approval-form">
      <h3>Approve Fuel Request</h3>
      
      <div className="form-group">
        <label>Status: {nextStatus}</label>
      </div>

      <div className="form-group">
        <label htmlFor="meter-reading">
          Meter Reading (liters) - Optional
        </label>
        <input
          id="meter-reading"
          type="number"
          step="0.01"
          value={meterReading ?? ''}
          onChange={(e) => setMeterReading(
            e.target.value ? parseFloat(e.target.value) : null
          )}
          placeholder="Enter tank level"
        />
      </div>

      {error && <div className="error">{error}</div>}

      <button 
        onClick={handleApprove}
        disabled={isSubmitting}
      >
        {isSubmitting ? 'Approving...' : 'Approve'}
      </button>
    </div>
  );
}
```

### Angular Example

```typescript
import { Component, Input } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface FuelApprovalRequest {
  status: string;
  meterReading?: number;
}

@Component({
  selector: 'app-fuel-approval',
  template: `
    <form (ngSubmit)="onApprove()">
      <h3>{{ nextStatus }}</h3>
      
      <div class="form-group">
        <label>Meter Reading (liters) - Optional</label>
        <input 
          type="number" 
          step="0.01"
          [(ngModel)]="meterReading" 
          name="meterReading"
          placeholder="Enter tank level"
        />
      </div>

      <div *ngIf="error" class="error">{{ error }}</div>

      <button type="submit" [disabled]="isSubmitting">
        {{ isSubmitting ? 'Approving...' : 'Approve' }}
      </button>
    </form>
  `
})
export class FuelApprovalComponent {
  @Input() fuelRecordId!: number;
  @Input() nextStatus!: string;
  
  meterReading: number | null = null;
  isSubmitting = false;
  error: string | null = null;

  constructor(private http: HttpClient) {}

  onApprove(): void {
    this.isSubmitting = true;
    this.error = null;

    const request: FuelApprovalRequest = { status: this.nextStatus };
    if (this.meterReading !== null) {
      request.meterReading = this.meterReading;
    }

    this.http.patch(`/api/fuel/${this.fuelRecordId}/status`, request)
      .subscribe({
        next: () => {
          console.log('Fuel approved successfully');
          // Handle success
        },
        error: (err) => {
          this.error = 'Failed to approve fuel request';
          console.error(err);
        },
        complete: () => {
          this.isSubmitting = false;
        }
      });
  }
}
```

## Database Migration

To apply the new column to an existing database, run the migration file:

```bash
# Run the migration script manually
mysql -u root -p transport < src/main/resources/db-migrations/V1_1_Add_Meter_Reading.sql
```

Or execute the SQL directly:

```sql
ALTER TABLE fuel_approval_log
ADD COLUMN meter_reading DECIMAL(10, 2) NULL
COMMENT 'Optional meter reading (e.g., fuel tank gauge) recorded by authorizer during approval';
```

## Querying Approval Records with Meter Readings

### Get All Approvals with Meter Readings

```sql
SELECT 
  id,
  fuel_record_id,
  action,
  actioned_by_username,
  actioned_at,
  vehicle,
  fuel_type,
  liters,
  meter_reading,
  CASE 
    WHEN meter_reading IS NOT NULL 
    THEN CONCAT(meter_reading, 'L')
    ELSE 'Not recorded'
  END as recorded_level
FROM fuel_approval_log
WHERE meter_reading IS NOT NULL
ORDER BY actioned_at DESC;
```

### Get Approvals by Status with Meter Readings

```sql
SELECT * 
FROM fuel_approval_log
WHERE action = 'ATTENDANT_APPROVED' 
  AND meter_reading IS NOT NULL
ORDER BY actioned_at DESC;
```

## Validation

### Meter Reading Constraints
- **Format:** Decimal number with up to 2 decimal places
- **Range:** 0 to 9999.99 (liters)
- **Required:** No (optional field)
- **Precision:** 10 digits total, 2 decimal places (e.g., 12345678.90)

### API Validation

The API automatically validates meter readings:
```json
{
  "meterReading": 450.25  // Valid
}
```

Invalid meter readings are silently ignored:
```json
{
  "meterReading": "not a number"  // Will be ignored
}
```

## Reporting

### Approval Report with Meter Readings

```sql
SELECT 
  fr.id,
  fr.fuel_type,
  fr.liters as requested_liters,
  fal.action,
  fal.actioned_by_username,
  fal.actioned_at,
  fal.meter_reading,
  v.number_plate as vehicle_plate
FROM fuel_approval_log fal
JOIN fuel_records fr ON fal.fuel_record_id = fr.id
LEFT JOIN vehicles v ON fr.vehicle_id = v.id
WHERE fal.action IN ('MANAGER_APPROVED', 'ATTENDANT_APPROVED', 'SIGNED')
  AND fal.meter_reading IS NOT NULL
ORDER BY fal.actioned_at DESC;
```

## Audit Trail

The meter reading is stored in the `fuel_approval_log` table and becomes part of the audit trail. Each approval action can include:
- Who approved (user, role)
- When it was approved (timestamp)
- What status was assigned
- **Optional meter reading at time of approval**

This creates a complete historical record of fuel management decisions.

## Best Practices

1. **Consistent Recording**: Establish guidelines for when meter readings should be recorded:
   - Always at ATTENDANT stage (when fuel is actually dispensed)
   - Optionally at MANAGER approval (initial tank check)
   - Optionally at FINANCE sign-off (final reconciliation)

2. **Accuracy**: Record meter readings accurately:
   - Use precise readings from gauges
   - Round to nearest 0.01 liter
   - Note any gauge calibration issues

3. **Audit Purpose**: Meter readings help:
   - Verify fuel discrepancies
   - Track actual consumption vs. records
   - Detect fuel theft or leaks
   - Reconcile tank levels over time

4. **System Integration**: Consider displaying:
   - Difference between meter readings and system records
   - Trend analysis of tank levels
   - Alerts for unusual drops in fuel levels

## Implementation Notes

### Files Modified
- `src/main/java/com/tadiwa/fuel_management/DTO/FuelStatusRequest.java` - Added meterReading field
- `src/main/java/com/tadiwa/fuel_management/Entity/FuelApprovalLog.java` - Added meter_reading column
- `src/main/java/com/tadiwa/fuel_management/Service/FuelService.java` - Updated approval methods to handle meter readings
- `src/main/java/com/tadiwa/fuel_management/Controller/FuelController.java` - Updated controller to pass meter readings
- `src/main/resources/db-migrations/V1_1_Add_Meter_Reading.sql` - Database migration

### Backward Compatibility

This feature is fully backward compatible:
- The meter_reading field is optional in the API
- Existing approval workflows continue to work without providing meter readings
- Database column is nullable, existing records remain unchanged

## Troubleshooting

### Meter Reading Not Saved
- Verify the database migration has been applied
- Check that the FuelApprovalLog entity has the meterReading field
- Ensure the API request includes the meterReading in the JSON body

### Authorization Issues
- Only authorized roles can approve fuel (check current user role)
- Meter readings can be provided by any authorized approver
- No additional permissions required beyond approval permissions

### Data Type Issues
- Meter readings must be numeric (decimal/float)
- Avoid sending as strings: use `450.25` not `"450.25"`
- Maximum precision: 10 digits with 2 decimal places
