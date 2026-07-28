package com.tadiwa.fuel_management.Controller;

import com.tadiwa.fuel_management.DTO.FuelRequest;
import com.tadiwa.fuel_management.DTO.VehicleRequest;
import com.tadiwa.fuel_management.DTO.VehicleResponse;
import com.tadiwa.fuel_management.DTO.VehicleSelectDTO;
import com.tadiwa.fuel_management.Service.FuelService;
import com.tadiwa.fuel_management.Service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "Vehicle management — requires Bearer token")
@SecurityRequirement(name = "Bearer Auth")
public class VehicleController {

    private final VehicleService vehicleService;
    private final FuelService fuelService;

    public VehicleController(VehicleService vehicleService, FuelService fuelService) {
        this.vehicleService = vehicleService;
        this.fuelService = fuelService;
    }

    /* ==============================
       ADD VEHICLE
    ============================== */
    @Operation(summary = "Add a new vehicle", description = "ADMIN or MANAGER only.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Vehicle added"),
        @ApiResponse(responseCode = "409", description = "Number plate or chassis number already exists"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    public ResponseEntity<VehicleResponse> addVehicle(@RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.addVehicle(request);

        if (!response.isSuccess()) {
            HttpStatus status = response.getMessage().contains("already exists")
                    ? HttpStatus.CONFLICT
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ==============================
       GET ALL VEHICLES
    ============================== */
    @Operation(summary = "Get all vehicles")
    @ApiResponse(responseCode = "200", description = "List of vehicles")
    @GetMapping
    public ResponseEntity<VehicleResponse> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    /* ==============================
       GET VEHICLE SELECT (DROPDOWN)
    ============================== */
    @Operation(summary = "Get vehicles for dropdown", description = "Returns id and number plate only")
    @GetMapping("/select")
    public ResponseEntity<List<VehicleSelectDTO>> getVehicleSelect() {
        return ResponseEntity.ok(vehicleService.getVehicleSelect());
    }

    /* ==============================
       GET VEHICLE BY ID
    ============================== */
    @Operation(summary = "Get vehicle by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle found"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Integer id) {
        VehicleResponse response = vehicleService.getVehicleById(id);

        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /* ==============================
       UPDATE VEHICLE
    ============================== */
    @Operation(summary = "Update a vehicle", description = "ADMIN or MANAGER only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found"),
        @ApiResponse(responseCode = "409", description = "Number plate or chassis number already exists")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Integer id,
            @RequestBody VehicleRequest request) {

        VehicleResponse response = vehicleService.updateVehicle(id, request);

        if (!response.isSuccess()) {
            String msg = response.getMessage();
            HttpStatus status = msg.contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : msg.contains("already exists")
                            ? HttpStatus.CONFLICT
                            : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /* ==============================
       DELETE VEHICLE
    ============================== */
    @Operation(summary = "Delete a vehicle", description = "ADMIN only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle deleted"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<VehicleResponse> deleteVehicle(@PathVariable Integer id) {
        VehicleResponse response = vehicleService.deleteVehicle(id);

        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /* ==============================
       SEARCH BY NUMBER PLATE
    ============================== */
    @Operation(summary = "Search vehicle by number plate")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicle found"),
        @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    @GetMapping("/search/plate/{numberPlate}")
    public ResponseEntity<VehicleResponse> searchByNumberPlate(@PathVariable String numberPlate) {
        VehicleResponse response = vehicleService.searchByNumberPlate(numberPlate);

        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /* ==============================
       SEARCH VEHICLES BY KEYWORD
    ============================== */
    @Operation(summary = "Search vehicles by keyword", description = "Searches across make, model, and number plate")
    @GetMapping("/search")
    public ResponseEntity<VehicleResponse> searchVehicles(@RequestParam String keyword) {
        return ResponseEntity.ok(vehicleService.searchVehicles(keyword));
    }

    /* ==============================
       GET VEHICLES BY MAKE
    ============================== */
    @Operation(summary = "Get vehicles by make")
    @GetMapping("/make/{make}")
    public ResponseEntity<VehicleResponse> getVehiclesByMake(@PathVariable String make) {
        return ResponseEntity.ok(vehicleService.getVehiclesByMake(make));
    }

    /* ==============================
       GET VEHICLES BY MODEL
    ============================== */
    @Operation(summary = "Get vehicles by model")
    @GetMapping("/model/{model}")
    public ResponseEntity<VehicleResponse> getVehiclesByModel(@PathVariable String model) {
        return ResponseEntity.ok(vehicleService.getVehiclesByModel(model));
    }

    /* ==============================
       ASSIGN VEHICLE (create fuel request)
    ============================== */
    @Operation(summary = "Driver assigns vehicle and submits fuel request",
               description = "DRIVER only. Creates a PENDING fuel record for the given vehicle. Required fields: name (user_name), department, fuel_type, liters, fuel_date. Optional: mileage, notes.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Fuel record created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Only DRIVER role can assign vehicles")
    })
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @PostMapping("/{vehicleId}/assign")
    public ResponseEntity<Map<String, Object>> assignVehicle(
            @PathVariable Integer vehicleId,
            @RequestBody FuelRequest request) {
        Long userId = fuelService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            request.setVehicleId(vehicleId);
            return ResponseEntity.status(HttpStatus.CREATED).body(fuelService.addFuel(request, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
