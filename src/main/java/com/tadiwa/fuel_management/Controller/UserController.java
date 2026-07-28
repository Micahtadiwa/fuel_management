package com.tadiwa.fuel_management.Controller;



import com.tadiwa.fuel_management.DTO.AuthResponse;
import com.tadiwa.fuel_management.DTO.LoginRequest;
import com.tadiwa.fuel_management.DTO.RegisterRequest;
import com.tadiwa.fuel_management.DTO.UserResponse;
import com.tadiwa.fuel_management.Entity.User;
import com.tadiwa.fuel_management.Repository.UserRepository;
import com.tadiwa.fuel_management.Security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register and login endpoints — no token required")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Email or username already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {

        String email = request.getEmail().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, "Email already exists"));
        }

        if (userRepository.existsByUsername(request.getName())) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, "Username already exists"));
        }

        User user = new User();
        user.setUsername(request.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsActive(true);
        user.setTermsAccepted(false);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return ResponseEntity.ok(
                new AuthResponse(token, user.getUsername(), user.getRole(), "Registration successful")
        );
    }

    @Operation(summary = "Login", description = "Authenticates a user and returns a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {

        String email = request.getEmail().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, "Invalid email or password"));
        }

        // Account pending checker approval
        if (user.isPendingApproval()) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null,
                            "Your account is pending approval. Please wait for an administrator to approve it."));
        }

        // Account locked due to too many failed attempts
        if (user.getFailedLoginAttempts() >= 3) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null,
                            "Your account has been locked due to too many failed login attempts. Please contact an administrator."));
        }

        // Manually deactivated account
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null,
                            "Your account is inactive. Please contact the administrator."));
        }

        boolean passwordValid;
        String stored = user.getPassword();
        if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$"))) {
            passwordValid = passwordEncoder.matches(request.getPassword(), stored);
        } else {
            // Plaintext legacy password — validate then upgrade to BCrypt
            passwordValid = request.getPassword().equals(stored);
            if (passwordValid) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);
            }
        }

        if (!passwordValid) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 3) {
                user.setIsActive(false);
                userRepository.save(user);
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(null, null, null,
                                "Your account has been locked due to too many failed login attempts. Please contact an administrator."));
            }
            userRepository.save(user);
            int remaining = 3 - attempts;
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null,
                            "Invalid email or password. " + remaining + " attempt(s) remaining before account lockout."));
        }

        // Successful login — reset failed attempts
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
        String token = jwtUtil.generateToken(user.getUsername(), role);

        return ResponseEntity.ok(
                new AuthResponse(token, user.getUsername(), role, "Login successful")
        );
    }

    @Operation(summary = "Get locked users", description = "Returns users locked due to 3 failed login attempts.")
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'ADMIN_CHECKER')")
    @GetMapping("/locked-users")
    public ResponseEntity<List<UserResponse>> getLockedUsers() {
        List<UserResponse> locked = userRepository.findByFailedLoginAttemptsGreaterThanEqual(3)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(locked);
    }

    @Operation(summary = "Get all users")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Create a user with a role (ADMIN_MAKER only)",
               description = "Creates a new user and assigns the specified role immediately.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Email/username already exists or invalid role")
    })
    @PreAuthorize("hasRole('ADMIN_MAKER')")
    @PostMapping("/admin/users")
    public ResponseEntity<?> adminCreateUser(@RequestBody RegisterRequest request) {
        String email = request.getEmail() != null ? request.getEmail().toLowerCase() : null;

        if (email == null || request.getName() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "name, email, and password are required"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email already exists"));
        }

        if (userRepository.existsByUsername(request.getName())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username already exists"));
        }

        String role = request.getRole() != null ? request.getRole().toUpperCase() : "USER";
        if (!List.of("USER", "ADMIN_MAKER", "ADMIN_CHECKER", "MANAGER", "ATTENDANT", "FINANCE", "DRIVER").contains(role)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid role. Allowed: USER, ADMIN_MAKER, ADMIN_CHECKER, MANAGER, ATTENDANT, FINANCE, DRIVER"));
        }

        User user = new User();
        user.setUsername(request.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsActive(false);
        user.setPendingApproval(true);
        user.setTermsAccepted(false);

        User saved = userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User created successfully. Awaiting ADMIN_CHECKER approval.",
                "userId", saved.getUserId(),
                "username", saved.getUsername(),
                "role", saved.getRole()
        ));
    }

    @Operation(summary = "Health check", description = "Confirms the API is running")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Fuel Management System Running");
    }

    @Operation(summary = "Update user active status", description = "Set isActive to true or false (ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "isActive field missing or invalid"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'ADMIN_CHECKER')")
    @PatchMapping("/users/{id}/active")
    public ResponseEntity<?> updateActiveStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Object val = body.get("isActive");
        if (val == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "isActive field is required"));
        }

        String valStr = val.toString().toLowerCase();
        if (!valStr.equals("true") && !valStr.equals("false")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "isActive must be true or false"));
        }

        boolean isActive = Boolean.parseBoolean(valStr);

        return userRepository.findById(id)
                .map(user -> {
                    user.setIsActive(isActive);
                    if (isActive) {
                        user.setFailedLoginAttempts(0);
                    }
                    user.setUpdatedAt(LocalDateTime.now());
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(Map.of(
                            "message", isActive ? "User activated successfully" : "User deactivated successfully",
                            "userId", saved.getUserId(),
                            "username", saved.getUsername(),
                            "isActive", saved.getIsActive()
                    ));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @Operation(summary = "Update user role (ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role updated"),
        @ApiResponse(responseCode = "400", description = "Invalid role"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN_MAKER')")
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String role = body.get("role") != null ? body.get("role").toString().toUpperCase() : null;

        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "role field is required"));
        }

        if (!List.of("USER", "ADMIN_MAKER", "ADMIN_CHECKER", "MANAGER", "ATTENDANT", "FINANCE", "DRIVER").contains(role)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid role. Allowed: USER, ADMIN_MAKER, ADMIN_CHECKER, MANAGER, ATTENDANT, FINANCE, DRIVER"));
        }

        return userRepository.findById(id)
                .map(user -> {
                    user.setRole(role);
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @Operation(summary = "Change own password", description = "Authenticated user changes their own password. Requires current password.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed"),
        @ApiResponse(responseCode = "400", description = "Wrong current password or missing fields")
    })
    @PatchMapping("/change-password")
    public ResponseEntity<?> changeOwnPassword(@RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");

        if (currentPassword == null || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "currentPassword and newPassword are required"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "New password must be at least 6 characters"));
        }

        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @Operation(summary = "Reset any user's password (ADMIN_MAKER only)", description = "Admin resets a user's password without needing the current one.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid fields"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN_MAKER')")
    @PatchMapping("/users/{id}/password")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String newPassword = body.get("newPassword");

        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "newPassword is required"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must be at least 6 characters"));
        }

        return userRepository.findById(id)
                .map(user -> {
                    boolean wasLocked = user.getFailedLoginAttempts() >= 3;
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setUpdatedAt(LocalDateTime.now());
                    if (wasLocked) {
                        user.setFailedLoginAttempts(0);
                        user.setIsActive(true);
                    }
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of(
                            "message", "Password reset successfully" + (wasLocked ? " and account unlocked" : "")
                    ));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @Operation(summary = "Get pending approval users (ADMIN_CHECKER only)")
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'ADMIN_CHECKER')")
    @GetMapping("/pending-users")
    public ResponseEntity<List<UserResponse>> getPendingUsers() {
        List<UserResponse> pending = userRepository.findAll().stream()
                .filter(u -> u.isPendingApproval())
                .map(UserResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    @Operation(summary = "Approve a pending user (ADMIN_CHECKER only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User approved"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN_CHECKER')")
    @PatchMapping("/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setPendingApproval(false);
                    user.setIsActive(true);
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of(
                            "message", "User approved successfully. They can now log in.",
                            "username", user.getUsername()
                    ));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @Operation(summary = "Reject a pending user (ADMIN_CHECKER only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User rejected"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN_CHECKER')")
    @PatchMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setPendingApproval(false);
                    user.setIsActive(false);
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of(
                            "message", "User rejected.",
                            "username", user.getUsername()
                    ));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @Operation(summary = "Database connection test")
    @GetMapping("/db-test")
    public ResponseEntity<?> testDatabase() {
        try {
            long count = userRepository.count();
            return ResponseEntity.ok("Database connected. Total users: " + count);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Database connection failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Refresh token", description = "Extends session by refreshing the current token (requires authentication)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized or session expired")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid authorization header"));
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }

        if (jwtUtil.isTokenInactive(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Session expired due to inactivity"));
        }

        String refreshedToken = jwtUtil.refreshToken(token);
        return ResponseEntity.ok(Map.of(
                "token", refreshedToken,
                "message", "Token refreshed successfully"
        ));
    }
}
