package com.tadiwa.fuel_management.Controller;

import com.tadiwa.fuel_management.DTO.DispenseRequest;
import com.tadiwa.fuel_management.DTO.FuelRequest;
import com.tadiwa.fuel_management.DTO.FuelStatusRequest;
import com.tadiwa.fuel_management.DTO.RefillRequest;
import com.tadiwa.fuel_management.Service.FuelService;
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
@RequestMapping("/api/fuel")
@Tag(name = "Fuel", description = "Fuel records, tank operations — requires Bearer token")
@SecurityRequirement(name = "Bearer Auth")
public class FuelController {

    private final FuelService fuelService;

    public FuelController(FuelService fuelService) {
        this.fuelService = fuelService;
    }

    /* ==============================
       ADD FUEL RECORD
    ============================== */
    @Operation(summary = "Submit a fuel request (DRIVER only)",
               description = "Driver submits a PENDING fuel request for a vehicle. Workflow: DRIVER submits → MANAGER approves → ATTENDANT approves (fuel dispensed) → FINANCE signs.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Fuel record created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Only DRIVER role can submit fuel requests")
    })
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN_MAKER')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> addFuel(@RequestBody FuelRequest request) {
        Long userId = fuelService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(fuelService.addFuel(request, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /* ==============================
       GET FUEL RECORDS
    ============================== */
    @Operation(summary = "Get all fuel records", description = "Returns all fuel records with vehicle and user details")
    @ApiResponse(responseCode = "200", description = "List of fuel records")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getFuelRecords() {
        try {
            return ResponseEntity.ok(fuelService.getFuelRecords());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("success", false, "message", e.getMessage())));
        }
    }

    /* ==============================
       UPDATE FUEL STATUS
    ============================== */
    @Operation(summary = "Advance fuel record through approval workflow",
               description = """
                       Multi-step approval workflow:
                       1. MANAGER sends MANAGER_APPROVED (from PENDING)
                       2. ATTENDANT sends ATTENDANT_APPROVED (from MANAGER_APPROVED) — fuel is dispensed from tank at this step
                       3. FINANCE sends SIGNED (from ATTENDANT_APPROVED)
                       Any authorized role can send DECLINED at their respective stage.
                       ADMIN can perform any step.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status, wrong stage, or insufficient fuel"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Role not authorized for this stage"),
        @ApiResponse(responseCode = "404", description = "Record not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'MANAGER', 'ATTENDANT', 'FINANCE')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateFuelStatus(
            @PathVariable Long id,
            @RequestBody FuelStatusRequest request) {
        Long userId = fuelService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            return ResponseEntity.ok(fuelService.updateFuelStatus(id, request.getStatus(), userId, request.getMeterReading(), request.getTankLevel()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Server error";
            if (msg.startsWith("Record not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", msg));
            }
            if (msg.startsWith("Insufficient fuel")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", msg));
        }
    }

    /* ==============================
       DISPENSE FUEL
    ============================== */
    @Operation(summary = "Dispense fuel from tank", description = "Deducts petrol from the tank. ADMIN or MANAGER only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fuel dispensed"),
        @ApiResponse(responseCode = "400", description = "Not enough fuel in tank"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'MANAGER')")
    @PostMapping("/dispense")
    public ResponseEntity<Map<String, Object>> dispenseFuel(@RequestBody DispenseRequest request) {
        Long userId = fuelService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            return ResponseEntity.ok(fuelService.dispenseFuel(request, userId));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Error dispensing fuel";
            if (msg.contains("Not enough fuel")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error dispensing fuel"));
        }
    }

    /* ==============================
       REFILL TANK
    ============================== */
    @Operation(summary = "Refill a fuel tank", description = "ADMIN, MANAGER, or ATTENDANT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tank refilled"),
        @ApiResponse(responseCode = "400", description = "Exceeds capacity or invalid liters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Tank not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'MANAGER', 'ATTENDANT')")
    @PostMapping("/tank/{id}/refill")
    public ResponseEntity<Map<String, Object>> refillTank(
            @PathVariable Integer id,
            @RequestBody RefillRequest request) {
        Long userId = fuelService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            return ResponseEntity.ok(fuelService.refillTank(id, request, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Server error";
            if (msg.startsWith("Tank not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", msg));
            }
            if (msg.startsWith("Exceeds tank capacity")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An error occurred while refilling the tank"));
        }
    }

    /* ==============================
       TANK REMOVALS REPORT
    ============================== */
    @Operation(summary = "Get tank removals report",
               description = "All APPROVED/SIGNED fuel records with the approver name. Accessible to MANAGER, ATTENDANT, ADMIN, FINANCE.")
    @ApiResponse(responseCode = "200", description = "Tank removals returned")
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'ADMIN_CHECKER', 'MANAGER', 'ATTENDANT', 'FINANCE')")
    @GetMapping("/tank-removals")
    public ResponseEntity<List<Map<String, Object>>> getTankRemovals() {
        try {
            return ResponseEntity.ok(fuelService.getTankRemovals());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("success", false, "message", e.getMessage())));
        }
    }

    /* ==============================
       TANK REFILL HISTORY
    ============================== */
    @Operation(summary = "Get tank refill history", description = "All refill (IN) transactions for petrol and diesel tanks.")
    @ApiResponse(responseCode = "200", description = "Refill history returned")
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'ADMIN_CHECKER', 'MANAGER', 'ATTENDANT', 'FINANCE')")
    @GetMapping("/tank-refills")
    public ResponseEntity<List<Map<String, Object>>> getTankRefills() {
        try {
            return ResponseEntity.ok(fuelService.getTankRefills());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("success", false, "message", e.getMessage())));
        }
    }

    /* ==============================
       APPROVAL LOG
    ============================== */
    @Operation(summary = "Get approval log", description = "All APPROVED / SIGNED / DECLINED actions. ADMIN or MANAGER only.")
    @ApiResponse(responseCode = "200", description = "Approval log returned")
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'ADMIN_CHECKER', 'MANAGER', 'FINANCE')")
    @GetMapping("/approval-log")
    public ResponseEntity<List<Map<String, Object>>> getApprovalLog() {
        try {
            return ResponseEntity.ok(fuelService.getApprovalLog());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("success", false, "message", e.getMessage())));
        }
    }

    /* ==============================
       GET TANK STATUS
    ============================== */
    @Operation(summary = "Get tank status", description = "Returns current level, capacity, and percentage full.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tank status returned"),
        @ApiResponse(responseCode = "404", description = "Tank not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN_MAKER', 'ADMIN_CHECKER', 'MANAGER', 'FINANCE', 'ATTENDANT')")
    @GetMapping("/tank/{id}")
    public ResponseEntity<Map<String, Object>> getTank(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(fuelService.getTank(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
