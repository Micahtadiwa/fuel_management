package com.tadiwa.fuel_management.DTO;

import com.tadiwa.fuel_management.Entity.User;

import java.time.LocalDateTime;

public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private int failedLoginAttempts;
    private boolean pendingApproval;

    public UserResponse(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.isActive = user.getIsActive();
        this.createdAt = user.getCreatedAt();
        this.failedLoginAttempts = user.getFailedLoginAttempts();
        this.pendingApproval = user.isPendingApproval();
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public boolean isPendingApproval() { return pendingApproval; }
}
