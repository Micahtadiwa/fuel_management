package com.tadiwa.fuel_management.DTO;

import java.math.BigDecimal;

public class RefillRequest {
    private Double liters;
    private BigDecimal meterReading;  // Optional meter reading in the tank
    private BigDecimal tankLevel;     // Optional tank level when refilling

    public Double getLiters() { return liters; }
    public void setLiters(Double liters) { this.liters = liters; }

    public BigDecimal getMeterReading() { return meterReading; }
    public void setMeterReading(BigDecimal meterReading) { this.meterReading = meterReading; }

    public BigDecimal getTankLevel() { return tankLevel; }
    public void setTankLevel(BigDecimal tankLevel) { this.tankLevel = tankLevel; }
}
