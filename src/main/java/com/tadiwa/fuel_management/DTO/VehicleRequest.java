package com.tadiwa.fuel_management.DTO;

public class VehicleRequest {
    private String numberPlate;
    private String make;
    private String model;
    private String chassisNumber;

    // Constructors
    public VehicleRequest() {}

    public VehicleRequest(String numberPlate, String make, String model, String chassisNumber) {
        this.numberPlate = numberPlate;
        this.make = make;
        this.model = model;
        this.chassisNumber = chassisNumber;
    }

    // Getters and Setters
    public String getNumberPlate() {
        return numberPlate;
    }

    public void setNumberPlate(String numberPlate) {
        this.numberPlate = numberPlate;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getChassisNumber() {
        return chassisNumber;
    }

    public void setChassisNumber(String chassisNumber) {
        this.chassisNumber = chassisNumber;
    }

    @Override
    public String toString() {
        return "VehicleRequest{" +
                "numberPlate='" + numberPlate + '\'' +
                ", make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", chassisNumber='" + chassisNumber + '\'' +
                '}';
    }
}