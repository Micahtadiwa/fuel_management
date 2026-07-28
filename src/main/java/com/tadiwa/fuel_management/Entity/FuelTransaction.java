package com.tadiwa.fuel_management.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_transactions")
public class FuelTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tank_id", nullable = false)
    private FuelTank fuelTank;

    @Column(name = "movement_type")
    private String movementType;

    @Column(name = "litres")
    private Double litres;

    @Column(name = "balance_after")
    private Double balanceAfter;

    @Column(name = "source")
    private String source;

    @Column(name = "reference")
    private String reference;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "meters", length = 20)
    private String meters;

    @Column(name = "meter_reading", precision = 10, scale = 2)
    private BigDecimal meterReading;

    @Column(name = "tank_level", precision = 10, scale = 2)
    private BigDecimal tankLevel;

    public FuelTransaction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FuelTank getFuelTank() { return fuelTank; }
    public void setFuelTank(FuelTank fuelTank) { this.fuelTank = fuelTank; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public Double getLitres() { return litres; }
    public void setLitres(Double litres) { this.litres = litres; }

    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMeters() { return meters; }
    public void setMeters(String meters) { this.meters = meters; }

    public BigDecimal getMeterReading() { return meterReading; }
    public void setMeterReading(BigDecimal meterReading) { this.meterReading = meterReading; }

    public BigDecimal getTankLevel() { return tankLevel; }
    public void setTankLevel(BigDecimal tankLevel) { this.tankLevel = tankLevel; }
}
