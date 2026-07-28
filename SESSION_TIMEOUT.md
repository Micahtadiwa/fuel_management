# Session Timeout Implementation (30-Minute Inactivity)

## Overview

The fuel management system now implements automatic session locking after **30 minutes of inactivity**. This is accomplished using a JWT-based activity tracking mechanism with a sliding window approach.

## How It Works

### Server-Side Behavior

1. **Token Generation**: When a user logs in or registers, a JWT token is issued with:
   - User credentials (username, role)
   - `lastActivity` timestamp (current time when token is generated)
   - Token expiration (24 hours)

2. **Activity Validation**: On each API request:
   - The server checks if the token is valid (signature and expiration)
   - The server checks if **30 minutes have passed** since the last activity timestamp
   - If inactive for 30+ minutes: request is rejected with `401 Unauthorized`
   - If active: token is refreshed with a new `lastActivity` timestamp

3. **Token Refresh**: A new token is sent back in the response header:
   - Header: `X-New-Token`
   - Contains updated `lastActivity` timestamp
   - Client must use this new token for subsequent requests

### Configuration

The inactivity timeout is configured in `application.properties`:

```properties
jwt.inactivity-timeout=1800000  # 30 minutes in milliseconds
```

To change the timeout, modify this value:
- 5 minutes: `300000`
- 10 minutes: `600000`
- 30 minutes: `1800000` (default)
- 1 hour: `3600000`

## Frontend Integration Guide

### JavaScript/TypeScript Example

```typescript
class AuthManager {
  private currentToken: string | null = null;

  // Intercept responses to extract new token
  async fetchWithTokenRefresh(
    url: string,
    options: RequestInit = {}
  ): Promise<Response> {
    const response = await fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        Authorization: `Bearer ${this.currentToken}`,
      },
    });

    // Check if server sent a refreshed token
    const newToken = response.headers.get("X-New-Token");
    if (newToken) {
      this.currentToken = newToken;
      localStorage.setItem("token", newToken);
    }

    // Handle session timeout
    if (response.status === 401) {
      const errorData = await response.json();
      if (
        errorData.error &&
        errorData.error.includes("inactivity")
      ) {
        this.handleSessionTimeout();
        return response;
      }
    }

    return response;
  }

  handleSessionTimeout(): void {
    this.currentToken = null;
    localStorage.removeItem("token");
    
    // Redirect to login page
    window.location.href = "/login";
    
    // Or show a notification:
    // alert("Your session has expired due to inactivity. Please log in again.");
  }

  setToken(token: string): void {
    this.currentToken = token;
    localStorage.setItem("token", token);
  }
}

// Usage
const authManager = new AuthManager();

// After login
const loginResponse = await fetch("/api/auth/login", {
  method: "POST",
  body: JSON.stringify({ email, password }),
});
const { token } = await loginResponse.json();
authManager.setToken(token);

// Make API calls with automatic token refresh
const response = await authManager.fetchWithTokenRefresh("/api/fuel/records");
```

### React Example with Interceptor

```typescript
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

// Request interceptor to add token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor to handle token refresh and session timeout
api.interceptors.response.use(
  (response) => {
    // Extract new token from response header
    const newToken = response.headers["x-new-token"];
    if (newToken) {
      localStorage.setItem("token", newToken);
    }
    return response;
  },
  (error) => {
    // Handle session timeout
    if (error.response?.status === 401) {
      const errorMessage = error.response?.data?.error || "";
      if (errorMessage.includes("inactivity")) {
        localStorage.removeItem("token");
        window.location.href = "/login";
        alert("Your session has expired due to inactivity. Please log in again.");
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

### Angular Example with Interceptor

```typescript
import { Injectable } from "@angular/core";
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpResponse,
} from "@angular/common/http";
import { Observable } from "rxjs";
import { tap } from "rxjs/operators";

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });
    }

    return next.handle(req).pipe(
      tap(
        (event: HttpEvent<any>) => {
          if (event instanceof HttpResponse) {
            const newToken = event.headers.get("X-New-Token");
            if (newToken) {
              this.authService.setToken(newToken);
            }
          }
        },
        (error: any) => {
          if (error.status === 401) {
            const errorMessage = error.error?.error || "";
            if (errorMessage.includes("inactivity")) {
              this.authService.logout();
              // Redirect to login
            }
          }
        }
      )
    );
  }
}
```

## API Endpoints

### Login
**POST** `/api/auth/login`
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
  "role": "USER",
  "message": "Login successful"
}
```

### Register
**POST** `/api/auth/register`
```json
{
  "name": "john_doe",
  "email": "user@example.com",
  "password": "password123"
}
```
Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "username": "john_doe",
  "role": "USER",
  "message": "Registration successful"
}
```

### Refresh Token (Optional)
**POST** `/api/auth/refresh`

Headers:
```
Authorization: Bearer {current_token}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "message": "Token refreshed successfully"
}
```

### Using Any Protected Endpoint
When you call any protected endpoint, the server automatically refreshes your token:

**Request:**
```
GET /api/fuel/records
Authorization: Bearer {old_token}
```

**Response Headers:**
```
X-New-Token: eyJhbGciOiJIUzUxMiJ9...
```

**Response Body:**
```json
{
  "records": [...]
}
```

## Session Timeout Error Responses

### Inactive Token (30+ minutes without activity)
```json
{
  "error": "Session expired due to inactivity"
}
```
Status: `401 Unauthorized`

### Invalid or Expired Token
```json
{
  "error": "Invalid or expired token"
}
```
Status: `401 Unauthorized`

## User Experience Flow

1. **User logs in** → Receives JWT token with `lastActivity` timestamp
2. **User makes API calls** → Server validates token, checks inactivity
   - If < 30 min inactive: Request succeeds, new token in `X-New-Token` header
   - If ≥ 30 min inactive: Request fails with 401, error includes "inactivity"
3. **Client receives 401 + inactivity error** → Clears stored token, redirects to login
4. **Client handles success** → Updates token from `X-New-Token` header for next request

## Implementation Details

### Token Structure
JWT tokens include:
```json
{
  "sub": "username",
  "role": "USER",
  "lastActivity": 1234567890,  // Unix timestamp when token was issued/refreshed
  "iat": 1234567890,           // Issued at
  "exp": 1234654290            // Expires in 24 hours
}
```

### Activity Tracking Logic
```
Time since last activity = Current time - lastActivity claim
Is inactive? = Time since last activity > 1800000 ms (30 minutes)
```

### Files Modified
- `src/main/resources/application.properties` - Added `jwt.inactivity-timeout`
- `src/main/java/com/tadiwa/fuel_management/Security/JwtUtil.java` - Added activity tracking
- `src/main/java/com/tadiwa/fuel_management/Security/JwtFilter.java` - Added inactivity validation
- `src/main/java/com/tadiwa/fuel_management/Controller/UserController.java` - Added refresh endpoint

## Testing

### Manual Testing with cURL

1. **Login and get token:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

2. **Use token immediately (should succeed):**
```bash
curl -X GET http://localhost:8080/api/fuel/records \
  -H "Authorization: Bearer {token}"
```

3. **Wait 30+ minutes and use same token (should fail):**
```bash
curl -X GET http://localhost:8080/api/fuel/records \
  -H "Authorization: Bearer {token}"
```

Response:
```json
{
  "error": "Session expired due to inactivity"
}
```

4. **Use refresh endpoint:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer {token}"
```

## Security Considerations

1. **Token Storage**: Avoid storing tokens in localStorage if possible; use httpOnly cookies where feasible
2. **HTTPS Only**: Always use HTTPS in production to prevent token interception
3. **Token Rotation**: Tokens are automatically rotated on each request (sliding window)
4. **Clock Skew**: The server uses system time; ensure server clocks are synchronized
5. **Logout**: Currently no explicit logout endpoint; token becomes invalid after 24 hours or 30 min inactivity

## Troubleshooting

### Issue: "Session expired due to inactivity" immediately after login
- **Cause**: System clock skew between client and server
- **Solution**: Ensure both client and server have synchronized system time

### Issue: User logged out after 30 minutes even with activity
- **Cause**: Frontend not extracting and updating token from `X-New-Token` header
- **Solution**: Implement token refresh in your HTTP interceptor (see examples above)

### Issue: Users stay logged in indefinitely
- **Cause**: Token expiration not being enforced, or frontend not checking response headers
- **Solution**: Verify JWT configuration and implement proper token handling

## Future Enhancements

1. Add explicit logout endpoint (blacklist tokens)
2. Add "remember me" functionality (extended timeout)
3. Add client-side countdown timer before session expires
4. Send notification before session timeout
5. Add activity-based metrics and logging
