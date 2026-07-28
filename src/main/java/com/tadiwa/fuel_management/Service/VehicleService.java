package com.tadiwa.fuel_management.Service;

import com.tadiwa.fuel_management.DTO.VehicleRequest;
import com.tadiwa.fuel_management.DTO.VehicleResponse;
import com.tadiwa.fuel_management.DTO.VehicleSelectDTO;
import com.tadiwa.fuel_management.Entity.Vehicle;
import com.tadiwa.fuel_management.Repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    // Add vehicle
    public VehicleResponse addVehicle(VehicleRequest request) {

        // Validate fields
        if (request.getNumberPlate() == null || request.getMake() == null ||
                request.getModel() == null || request.getChassisNumber() == null) {
            return new VehicleResponse(false, "All fields are required");
        }

        String numberPlate = request.getNumberPlate().trim();
        String make = request.getMake().trim();
        String model = request.getModel().trim();
        String chassisNumber = request.getChassisNumber().trim();

        // Check empty fields
        if (numberPlate.isEmpty() || make.isEmpty() ||
                model.isEmpty() || chassisNumber.isEmpty()) {
            return new VehicleResponse(false, "Fields cannot be empty");
        }

        // Check duplicates
        if (vehicleRepository.existsByNumberPlate(numberPlate)) {
            return new VehicleResponse(false, "Number plate already exists");
        }

        if (vehicleRepository.existsByChassisNumber(chassisNumber)) {
            return new VehicleResponse(false, "Chassis number already exists");
        }

        // Create and save vehicle
        Vehicle vehicle = new Vehicle();
        vehicle.setNumberPlate(numberPlate.toUpperCase());
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setChassisNumber(chassisNumber);

        Vehicle saved = vehicleRepository.save(vehicle);

        return new VehicleResponse(true, "Vehicle added successfully", saved);
    }

    // Get all vehicles
    public VehicleResponse getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        return new VehicleResponse(true, "Vehicles retrieved successfully", vehicles);
    }

    // Get vehicle by ID
    public VehicleResponse getVehicleById(Integer id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElse(null);

        if (vehicle == null) {
            return new VehicleResponse(false, "Vehicle not found");
        }

        return new VehicleResponse(true, "Vehicle found", vehicle);
    }

    // Get vehicle select (for dropdowns)
    public List<VehicleSelectDTO> getVehicleSelect() {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        return vehicles.stream()
                .map(v -> new VehicleSelectDTO(v.getId(), v.getNumberPlate()))
                .collect(Collectors.toList());
    }

    // Update vehicle
    public VehicleResponse updateVehicle(Integer id, VehicleRequest request) {
        if (request.getNumberPlate() == null || request.getMake() == null ||
                request.getModel() == null || request.getChassisNumber() == null) {
            return new VehicleResponse(false, "All fields are required");
        }

        String numberPlate = request.getNumberPlate().trim().toUpperCase();
        String make = request.getMake().trim();
        String model = request.getModel().trim();
        String chassisNumber = request.getChassisNumber().trim();

        if (numberPlate.isEmpty() || make.isEmpty() || model.isEmpty() || chassisNumber.isEmpty()) {
            return new VehicleResponse(false, "Fields cannot be empty");
        }

        Vehicle vehicle = vehicleRepository.findById(id).orElse(null);
        if (vehicle == null) {
            return new VehicleResponse(false, "Vehicle not found");
        }

        if (!vehicle.getNumberPlate().equals(numberPlate) && vehicleRepository.existsByNumberPlate(numberPlate)) {
            return new VehicleResponse(false, "Number plate already exists");
        }

        if (!vehicle.getChassisNumber().equals(chassisNumber) && vehicleRepository.existsByChassisNumber(chassisNumber)) {
            return new VehicleResponse(false, "Chassis number already exists");
        }

        vehicle.setNumberPlate(numberPlate);
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setChassisNumber(chassisNumber);

        Vehicle updated = vehicleRepository.save(vehicle);
        return new VehicleResponse(true, "Vehicle updated successfully", updated);
    }

    // Delete vehicle
    public VehicleResponse deleteVehicle(Integer id) {
        if (!vehicleRepository.existsById(id)) {
            return new VehicleResponse(false, "Vehicle not found");
        }

        vehicleRepository.deleteById(id);

        return new VehicleResponse(true, "Vehicle deleted successfully");
    }

    // Search by number plate
    public VehicleResponse searchByNumberPlate(String numberPlate) {
        Vehicle vehicle = vehicleRepository.findByNumberPlate(numberPlate.toUpperCase()).orElse(null);

        if (vehicle == null) {
            return new VehicleResponse(false, "Vehicle not found");
        }

        return new VehicleResponse(true, "Vehicle found", vehicle);
    }

    // Search vehicles by keyword
    public VehicleResponse searchVehicles(String keyword) {
        List<Vehicle> vehicles = vehicleRepository.searchVehicles(keyword);

        return new VehicleResponse(true, "Search completed", vehicles);
    }

    // Get vehicles by make
    public VehicleResponse getVehiclesByMake(String make) {
        List<Vehicle> vehicles = vehicleRepository.findByMake(make);

        return new VehicleResponse(true, "Vehicles retrieved", vehicles);
    }

    // Get vehicles by model
    public VehicleResponse getVehiclesByModel(String model) {
        List<Vehicle> vehicles = vehicleRepository.findByModel(model);

        return new VehicleResponse(true, "Vehicles retrieved", vehicles);
    }
}