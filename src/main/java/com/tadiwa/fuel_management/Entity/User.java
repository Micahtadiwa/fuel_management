package com.tadiwa.fuel_management.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "Password")
    private String password;

    @Column(name = "role")
    private String role = "USER";

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "pending_approval")
    private boolean pendingApproval = false;

    // Backtick-quoted to prevent SpringPhysicalNamingStrategy from converting
    // PascalCase → snake_case and creating duplicate columns in the DB
    @Column(name = "`CreatedAt`")
    private LocalDateTime createdAt;

    @Column(name = "`UpdatedAt`")
    private LocalDateTime updatedAt;

    @Column(name = "`IsActive`")
    private Boolean isActive = true;

    @Column(name = "`TermsAccepted`")
    private Boolean termsAccepted = false;

    // Hibernate-generated snake_case duplicate — kept in sync via setters
    @Column(name = "is_active", columnDefinition = "BIT(1)")
    private Boolean isActiveFlag = false;

    @Column(name = "terms_accepted", columnDefinition = "BIT(1)")
    private Boolean termsAcceptedFlag = false;

    // Constructors
    public User() {}

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.isActiveFlag = isActive; // keep both columns in sync
    }

    public Boolean getTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(Boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
        this.termsAcceptedFlag = termsAccepted; // keep both columns in sync
    }

    public Boolean getIsActiveFlag() { return isActiveFlag; }
    public void setIsActiveFlag(Boolean isActiveFlag) { this.isActiveFlag = isActiveFlag; }

    public Boolean getTermsAcceptedFlag() { return termsAcceptedFlag; }
    public void setTermsAcceptedFlag(Boolean termsAcceptedFlag) { this.termsAcceptedFlag = termsAcceptedFlag; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public boolean isPendingApproval() { return pendingApproval; }
    public void setPendingApproval(boolean pendingApproval) { this.pendingApproval = pendingApproval; }
}
