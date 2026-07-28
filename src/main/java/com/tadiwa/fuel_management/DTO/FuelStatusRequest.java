package com.tadiwa.fuel_management.DTO;

import java.math.BigDecimal;

public class FuelStatusRequest {
    private String status;
    private BigDecimal meterReading;  // Optional meter reading in the tank
    private BigDecimal tankLevel;     // Optional tank level during approval

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getMeterReading() { return meterReading; }
    public void setMeterReading(BigDecimal meterReading) { this.meterReading = meterReading; }

    public BigDecimal getTankLevel() { return tankLevel; }
    public void setTankLevel(BigDecimal tankLevel) { this.tankLevel = tankLevel; }
}
