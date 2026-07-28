# Complete Refill Fix - Backend & Frontend

## Backend Status ✅

### What's Fixed:
1. ✅ `FuelTransaction` entity - Added `meterReading` and `tankLevel` fields
2. ✅ `FuelApprovalLog` entity - Added `tankLevel` field  
3. ✅ All DTOs updated with new fields
4. ✅ `FuelService` - Logs transactions with meter readings and tank levels
5. ✅ `refillTank()` method - Correctly adds fuel to tank
6. ✅ API endpoints - Return `meterReading` and `tankLevel` in responses
7. ✅ `DatabaseInitializer` - Auto-creates missing columns on startup
8. ✅ Authorization - Refill now available to `ADMIN_MAKER`, `MANAGER`, `ATTENDANT`

### Backend Refill Flow:
```
1. User calls POST /api/fuel/tank/1/refill with {"liters": 500, "meterReading": 45000, "tankLevel": 95}
2. Service gets current tank level from latest transaction
3. Calculates: newLevel = currentLevel + 500
4. Validates: newLevel <= capacity
5. Creates FuelTransaction with balanceAfter = newLevel
6. Returns: {"success": true, "current_level": newLevel, ...}
7. Next call to getTank() or getTankRefills() shows updated level
```

## Frontend Required Changes 🔧

Your Angular app needs to:

### 1. Set Correct API URL

**File**: `src/environments/environment.ts` (or your API service)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'  // ← Change 4200 to 8080
};
```

### 2. Refill Service

**File**: `src/app/services/fuel.service.ts` (or similar)

```typescript
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FuelService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  refillTank(tankId: number, request: {
    liters: number;
    meterReading?: number;
    tankLevel?: number;
  }): Observable<any> {
    const token = localStorage.getItem('auth_token'); // Or however you store the token
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    return this.http.post(
      `${this.apiUrl}/fuel/tank/${tankId}/refill`,
      request,
      { headers }
    );
  }

  getTankRefills(): Observable<any[]> {
    const token = localStorage.getItem('auth_token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<any[]>(
      `${this.apiUrl}/fuel/tank-refills`,
      { headers }
    );
  }

  getTankStatus(tankId: number): Observable<any> {
    const token = localStorage.getItem('auth_token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get(
      `${this.apiUrl}/fuel/tank/${tankId}`,
      { headers }
    );
  }
}
```

### 3. Refill Component

**File**: `src/app/components/refill-tank.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { FuelService } from '../services/fuel.service';

@Component({
  selector: 'app-refill-tank',
  templateUrl: './refill-tank.component.html',
  styleUrls: ['./refill-tank.component.css']
})
export class RefillTankComponent implements OnInit {
  tankId: number = 1; // Or get from route/dropdown
  liters: number = 0;
  meterReading: number | null = null;
  tankLevel: number | null = null;
  loading: boolean = false;
  message: string = '';
  error: string = '';
  currentLevel: number = 0;
  capacity: number = 0;

  constructor(private fuelService: FuelService) {}

  ngOnInit(): void {
    this.getTankStatus();
  }

  getTankStatus(): void {
    this.fuelService.getTankStatus(this.tankId).subscribe(
      (response: any) => {
        this.currentLevel = response.current_level;
        this.capacity = response.capacity;
      },
      (error) => {
        this.error = 'Failed to load tank status: ' + error.message;
      }
    );
  }

  onSubmit(): void {
    if (this.liters <= 0) {
      this.error = 'Liters must be greater than 0';
      return;
    }

    const maxAllowed = this.capacity - this.currentLevel;
    if (this.liters > maxAllowed) {
      this.error = `Cannot add that much fuel. Max allowed: ${maxAllowed}L`;
      return;
    }

    this.loading = true;
    this.error = '';
    this.message = '';

    const request = {
      liters: this.liters,
      meterReading: this.meterReading || undefined,
      tankLevel: this.tankLevel || undefined
    };

    this.fuelService.refillTank(this.tankId, request).subscribe(
      (response: any) => {
        this.loading = false;
        if (response.success) {
          this.message = `Tank refilled successfully! New level: ${response.current_level}L (${response.percent_full}%)`;
          this.currentLevel = response.current_level;
          this.liters = 0;
          this.meterReading = null;
          this.tankLevel = null;
        } else {
          this.error = response.message || 'Refill failed';
        }
      },
      (error) => {
        this.loading = false;
        if (error.status === 403) {
          this.error = 'Access denied. You do not have permission to refill tanks.';
        } else if (error.status === 404) {
          this.error = 'Tank not found';
        } else if (error.status === 400) {
          this.error = error.error?.message || 'Invalid request';
        } else {
          this.error = 'Error refilling tank: ' + (error.error?.message || error.message);
        }
      }
    );
  }
}
```

### 4. Refill Template

**File**: `src/app/components/refill-tank.component.html`

```html
<div class="refill-container">
  <h2>Refill Fuel Tank</h2>

  <div class="tank-status">
    <p>Current Level: <strong>{{ currentLevel }}L</strong> / {{ capacity }}L</p>
    <p>Can add: <strong>{{ capacity - currentLevel }}L</strong> maximum</p>
  </div>

  <form (ngSubmit)="onSubmit()" #form="ngForm">
    <div class="form-group">
      <label for="liters">Liters to Add *</label>
      <input
        type="number"
        id="liters"
        name="liters"
        [(ngModel)]="liters"
        [max]="capacity - currentLevel"
        step="0.01"
        required
        placeholder="Enter liters"
      />
    </div>

    <div class="form-group">
      <label for="meterReading">Meter Reading (Optional)</label>
      <input
        type="number"
        id="meterReading"
        name="meterReading"
        [(ngModel)]="meterReading"
        step="0.01"
        placeholder="e.g., 45000.50"
      />
    </div>

    <div class="form-group">
      <label for="tankLevel">Tank Level % (Optional)</label>
      <input
        type="number"
        id="tankLevel"
        name="tankLevel"
        [(ngModel)]="tankLevel"
        step="0.1"
        max="100"
        placeholder="e.g., 95.5"
      />
    </div>

    <div class="actions">
      <button type="submit" [disabled]="loading || !form.valid">
        {{ loading ? 'Refilling...' : 'Refill Tank' }}
      </button>
      <button type="button" (click)="getTankStatus()">Refresh Status</button>
    </div>
  </form>

  <div *ngIf="message" class="alert alert-success">{{ message }}</div>
  <div *ngIf="error" class="alert alert-error">{{ error }}</div>
</div>
```

## Step-by-Step Setup

### Backend:
```bash
cd C:\Users\tmicah\Downloads\fuel_management

# 1. Build (currently building...)
mvn clean package -q -DskipTests

# 2. Run
mvn spring-boot:run
```

### Frontend:
```bash
# 1. Update environment.ts with http://localhost:8080/api
# 2. Update your API service to use correct URL
# 3. Update refill component code

# 4. Rebuild
ng serve  # or npm start
# or if using Angular CLI
ng build
```

## Testing

1. **Backend running**: `http://localhost:8080`
2. **Frontend running**: `http://localhost:4200`
3. **Login as**: attendant (or admin/manager)
4. **Go to**: Refill Tank page
5. **Enter**: 500 liters
6. **Click**: Refill Tank
7. **Expected**: Success message with new level

## Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| 403 Forbidden | Check role is ADMIN_MAKER, MANAGER, or ATTENDANT |
| 404 Not Found | Use correct tank ID (1=petrol, 2=diesel) |
| CORS Error | Add `Access-Control-Allow-Origin: *` header to backend |
| Tank level not increasing | Check API URL - should be localhost:8080, not 4200 |
| Token expired | Login again to get new token |

## Verification

After refilling, check:
```bash
# Tank status
curl http://localhost:8080/api/fuel/tank/1 \
  -H "Authorization: Bearer $TOKEN"

# Refill history
curl http://localhost:8080/api/fuel/tank-refills \
  -H "Authorization: Bearer $TOKEN"
```

Both should show the new fuel level!
