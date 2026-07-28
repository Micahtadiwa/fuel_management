package com.tadiwa.fuel_management.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class FuelRequest {

    @JsonProperty("vehicle_id")
    private Integer vehicleId;

    @JsonProperty("fuel_type")
    private String fuelType;

    private Double liters;
    private Double mileage;

    @JsonProperty("fuel_date")
    private LocalDate fuelDate;

    private String notes;

    @JsonProperty("user_name")
    private String userName;

    private String department;
    private String assignvehicles;

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public Double getLiters() { return liters; }
    public void setLiters(Double liters) { this.liters = liters; }

    public Double getMileage() { return mileage; }
    public void setMileage(Double mileage) { this.mileage = mileage; }

    public LocalDate getFuelDate() { return fuelDate; }
    public void setFuelDate(LocalDate fuelDate) { this.fuelDate = fuelDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getAssignvehicles() { return assignvehicles; }
    public void setAssignvehicles(String assignvehicles) { this.assignvehicles = assignvehicles; }
}
