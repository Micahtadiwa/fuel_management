package com.tadiwa.fuel_management.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class DispenseRequest {
    private Double liters;

    @JsonProperty("fuel_type")
    private String fuelType;

    private BigDecimal meterReading;  // Optional meter reading in the tank
    private BigDecimal tankLevel;     // Optional tank level when dispensing

    public Double getLiters() { return liters; }
    public void setLiters(Double liters) { this.liters = liters; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public BigDecimal getMeterReading() { return meterReading; }
    public void setMeterReading(BigDecimal meterReading) { this.meterReading = meterReading; }

    public BigDecimal getTankLevel() { return tankLevel; }
    public void setTankLevel(BigDecimal tankLevel) { this.tankLevel = tankLevel; }
}
