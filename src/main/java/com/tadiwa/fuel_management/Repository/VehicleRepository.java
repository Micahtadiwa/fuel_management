package com.tadiwa.fuel_management.Repository;

import com.tadiwa.fuel_management.Entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    // Find by number plate
    Optional<Vehicle> findByNumberPlate(String numberPlate);

    // Find by chassis number
    Optional<Vehicle> findByChassisNumber(String chassisNumber);

    // Check if number plate exists
    boolean existsByNumberPlate(String numberPlate);

    // Check if chassis number exists
    boolean existsByChassisNumber(String chassisNumber);

    // Find by make
    List<Vehicle> findByMake(String make);

    // Find by model
    List<Vehicle> findByModel(String model);

    // Find by make and model
    List<Vehicle> findByMakeAndModel(String make, String model);

    // Custom query: Search vehicles by keyword (number plate, make, or model)
    @Query("SELECT v FROM Vehicle v WHERE " +
            "LOWER(v.numberPlate) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.make) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.model) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Vehicle> searchVehicles(String keyword);
}