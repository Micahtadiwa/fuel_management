package com.tadiwa.fuel_management.DTO;

public class VehicleResponse {
    private boolean success;
    private String message;
    private Object data;

    // Constructors
    public VehicleResponse() {}

    public VehicleResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public VehicleResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}