package com.tadiwa.fuel_management.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_approval_log")
public class FuelApprovalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fuel_record_id", nullable = false)
    private Long fuelRecordId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;  // APPROVED | SIGNED | DECLINED

    @Column(name = "actioned_by_user_id")
    private Long actionedByUserId;

    @Column(name = "actioned_by_username")
    private String actionedByUsername;

    @Column(name = "actioned_by_role", length = 100)
    private String actionedByRole;

    @Column(name = "actioned_at", insertable = false, updatable = false)
    private LocalDateTime actionedAt;

    @Column(name = "vehicle")
    private String vehicle;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @Column(name = "liters", precision = 10, scale = 2)
    private BigDecimal liters;

    @Column(name = "fuel_date")
    private LocalDate fuelDate;

    @Column(name = "meter_reading", precision = 10, scale = 2)
    private BigDecimal meterReading;

    @Column(name = "tank_level", precision = 10, scale = 2)
    private BigDecimal tankLevel;

    public FuelApprovalLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFuelRecordId() { return fuelRecordId; }
    public void setFuelRecordId(Long fuelRecordId) { this.fuelRecordId = fuelRecordId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getActionedByUserId() { return actionedByUserId; }
    public void setActionedByUserId(Long actionedByUserId) { this.actionedByUserId = actionedByUserId; }

    public String getActionedByUsername() { return actionedByUsername; }
    public void setActionedByUsername(String actionedByUsername) { this.actionedByUsername = actionedByUsername; }

    public String getActionedByRole() { return actionedByRole; }
    public void setActionedByRole(String actionedByRole) { this.actionedByRole = actionedByRole; }

    public LocalDateTime getActionedAt() { return actionedAt; }

    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public BigDecimal getLiters() { return liters; }
    public void setLiters(BigDecimal liters) { this.liters = liters; }

    public LocalDate getFuelDate() { return fuelDate; }
    public void setFuelDate(LocalDate fuelDate) { this.fuelDate = fuelDate; }

    public BigDecimal getMeterReading() { return meterReading; }
    public void setMeterReading(BigDecimal meterReading) { this.meterReading = meterReading; }

    public BigDecimal getTankLevel() { return tankLevel; }
    public void setTankLevel(BigDecimal tankLevel) { this.tankLevel = tankLevel; }
}
