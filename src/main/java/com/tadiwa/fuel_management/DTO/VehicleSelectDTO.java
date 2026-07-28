package com.tadiwa.fuel_management.DTO;

public class VehicleSelectDTO {
    private Integer id;
    private String regNumber;

    // Constructors
    public VehicleSelectDTO() {}

    public VehicleSelectDTO(Integer id, String regNumber) {
        this.id = id;
        this.regNumber = regNumber;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }
}