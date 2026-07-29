package com.tadiwa.fuel_management.Service;

import com.tadiwa.fuel_management.DTO.FuelRequest;
import com.tadiwa.fuel_management.DTO.DispenseRequest;
import com.tadiwa.fuel_management.DTO.RefillRequest;
import com.tadiwa.fuel_management.Entity.FuelTank;


import com.tadiwa.fuel_management.Entity.FuelApprovalLog;
import com.tadiwa.fuel_management.Entity.FuelRecord;
import com.tadiwa.fuel_management.Entity.FuelTransaction;
import com.tadiwa.fuel_management.Entity.User;
import com.tadiwa.fuel_management.Repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FuelService {

    private final FuelRecordRepository fuelRecordRepository;
    private final FuelTankRepository fuelTankRepository;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final UserRepository userRepository;
    private final FuelApprovalLogRepository approvalLogRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate txTemplate;

    public FuelService(FuelRecordRepository fuelRecordRepository,
                       FuelTankRepository fuelTankRepository,
                       FuelTransactionRepository fuelTransactionRepository,
                       UserRepository userRepository,
                       FuelApprovalLogRepository approvalLogRepository,
                       JdbcTemplate jdbcTemplate,
                       PlatformTransactionManager transactionManager) {
        this.fuelRecordRepository = fuelRecordRepository;
        this.fuelTankRepository = fuelTankRepository;
        this.fuelTransactionRepository = fuelTransactionRepository;
        this.userRepository = userRepository;
        this.approvalLogRepository = approvalLogRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────

    private void saveApprovalLog(FuelRecord record, String action, Long userId, String vehiclePlate) {
        saveApprovalLog(record, action, userId, vehiclePlate, null, null);
    }

    private void saveApprovalLog(FuelRecord record, String action, Long userId, String vehiclePlate, BigDecimal meterReading) {
        saveApprovalLog(record, action, userId, vehiclePlate, meterReading, null);
    }

    private void saveApprovalLog(FuelRecord record, String action, Long userId, String vehiclePlate, BigDecimal meterReading, BigDecimal tankLevel) {
        try {
            User actor = userId != null
                    ? userRepository.findById(userId).orElse(null)
                    : null;

            FuelApprovalLog log = new FuelApprovalLog();
            log.setFuelRecordId(record.getId());
            log.setAction(action);
            log.setActionedByUserId(userId);
            log.setActionedByUsername(actor != null ? actor.getUsername() : "Unknown");
            log.setActionedByRole(actor != null ? actor.getRole() : "Unknown");
            log.setVehicle(vehiclePlate);
            log.setFuelType(record.getFuelType());
            log.setLiters(record.getLiters() != null
                    ? BigDecimal.valueOf(record.getLiters()) : null);
            log.setFuelDate(record.getFuelDate());
            log.setMeterReading(meterReading);
            log.setTankLevel(tankLevel);
            approvalLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Approval log save failed: " + e.getMessage());
        }
    }

    public Long getCurrentUserId() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(username).map(u -> u.getUserId()).orElse(null);
    }

    public String getCurrentUserRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    private double getResolvedTankLevel(Integer tankId) {
        List<FuelTransaction> latest = fuelTransactionRepository.findLatestByTankId(
                tankId, PageRequest.of(0, 1));
        if (latest.isEmpty()) return 0.0;
        Double bal = latest.get(0).getBalanceAfter();
        return bal != null ? bal : 0.0;
    }

    private void logFuelTransaction(Integer tankId, String movementType, double liters,
                                    double balanceAfter, String source, String reference, Long createdBy) {
        logFuelTransaction(tankId, movementType, liters, balanceAfter, source, reference, createdBy, null, null);
    }

    private void logFuelTransaction(Integer tankId, String movementType, double liters,
                                    double balanceAfter, String source, String reference, Long createdBy, BigDecimal meterReading) {
        logFuelTransaction(tankId, movementType, liters, balanceAfter, source, reference, createdBy, meterReading, null);
    }

    private void logFuelTransaction(Integer tankId, String movementType, double liters,
                                    double balanceAfter, String source, String reference, Long createdBy, BigDecimal meterReading, BigDecimal tankLevel) {
        FuelTank tank = fuelTankRepository.findById(tankId)
                .orElseThrow(() -> new RuntimeException("Tank not found"));
        FuelTransaction tx = new FuelTransaction();
        tx.setFuelTank(tank);
        tx.setMovementType(movementType);
        tx.setLitres(liters);
        tx.setBalanceAfter(balanceAfter);
        tx.setSource(source);
        tx.setReference(reference);
        tx.setCreatedBy(createdBy);
        tx.setMeterReading(meterReading);
        tx.setTankLevel(tankLevel);
        fuelTransactionRepository.save(tx);
    }

    // ─────────────────────────────────────────────
    //  ADD FUEL RECORD  (creates a PENDING request)
    // ─────────────────────────────────────────────

    public Map<String, Object> addFuel(FuelRequest req, Long userId) {
        if (userId == null) throw new SecurityException("Unauthorized");

        if (req.getVehicleId() == null || req.getFuelType() == null
                || req.getLiters() == null || req.getFuelDate() == null) {
            throw new IllegalArgumentException("Vehicle, fuel type, liters, and fuel date are required");
        }

        if (req.getUserName() == null || req.getUserName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        if (req.getDepartment() == null || req.getDepartment().isBlank()) {
            throw new IllegalArgumentException("department is required");
        }

        if (!List.of("petrol", "diesel").contains(req.getFuelType().toLowerCase())) {
            throw new IllegalArgumentException("Fuel type must be petrol or diesel");
        }

        User actor = userRepository.findById(userId).orElse(null);

        FuelRecord record = new FuelRecord();
        record.setVehicleId(req.getVehicleId());
        record.setUserId(userId);
        record.setFuelType(req.getFuelType());
        record.setLiters(req.getLiters());
        record.setMileage(req.getMileage());
        record.setFuelDate(req.getFuelDate());
        record.setNotes(req.getNotes());
        record.setStatus("PENDING");
        record.setUserName(req.getUserName());
        record.setDepartment(req.getDepartment());
        record.setAssignVehicles(req.getAssignvehicles());

        FuelRecord saved = fuelRecordRepository.save(record);

        return Map.of("success", true, "message", "Fuel record added successfully", "fuelId", saved.getId());
    }

    // ─────────────────────────────────────────────
    //  GET FUEL RECORDS
    // ─────────────────────────────────────────────

    public List<Map<String, Object>> getFuelRecords() {
        String sql = """
                SELECT
                  fr.id,
                  fr.vehicle_id                         AS vehicleId,
                  fr.user_id,
                  fr.fuel_type,
                  fr.liters,
                  fr.mileage,
                  fr.fuel_date,
                  fr.notes,
                  fr.created_at,
                  COALESCE(UPPER(fr.status), 'PENDING') AS status,
                  fr.user_name,
                  fr.department,
                  fr.assignvehicles,
                  u.Username                            AS userName,
                  v.number_plate                        AS vehicle,
                  v.number_plate                        AS regNumber,
                  v.make,
                  v.model,
                  al_mgr.actioned_by_username           AS approvedByManager,
                  al_att.actioned_by_username           AS approvedByAttendant,
                  al_fin.actioned_by_username           AS signedByFinance
                FROM fuel_records fr
                LEFT JOIN users u ON fr.user_id = u.UserID
                LEFT JOIN vehicles v ON fr.vehicle_id = v.id
                LEFT JOIN fuel_approval_log al_mgr
                       ON al_mgr.fuel_record_id = fr.id AND al_mgr.action = 'MANAGER_APPROVED'
                LEFT JOIN fuel_approval_log al_att
                       ON al_att.fuel_record_id = fr.id AND al_att.action = 'ATTENDANT_APPROVED'
                LEFT JOIN fuel_approval_log al_fin
                       ON al_fin.fuel_record_id = fr.id AND al_fin.action = 'SIGNED'
                ORDER BY fr.fuel_date DESC, fr.id DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }

    // ─────────────────────────────────────────────
    //  UPDATE FUEL STATUS  (approve / decline)
    // ─────────────────────────────────────────────

    // Workflow: DRIVER submits → PENDING
    //           MANAGER approves → MANAGER_APPROVED
    //           ATTENDANT approves (tank deducted) → ATTENDANT_APPROVED
    //           FINANCE signs → SIGNED
    //           Any role at their stage can DECLINE
    public Map<String, Object> updateFuelStatus(Long id, String statusInput, Long userId) {
        return updateFuelStatus(id, statusInput, userId, null, null);
    }

    public Map<String, Object> updateFuelStatus(Long id, String statusInput, Long userId, BigDecimal meterReading) {
        return updateFuelStatus(id, statusInput, userId, meterReading, null);
    }

    public Map<String, Object> updateFuelStatus(Long id, String statusInput, Long userId, BigDecimal meterReading, BigDecimal tankLevel) {
        String newStatus = (statusInput != null ? statusInput : "").toUpperCase();
        String actorRole = getCurrentUserRole();

        if (!List.of("MANAGER_APPROVED", "ATTENDANT_APPROVED", "SIGNED", "DECLINED").contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status. Allowed transitions: MANAGER_APPROVED, ATTENDANT_APPROVED, SIGNED, DECLINED");
        }

        FuelRecord record = fuelRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        String currentStatus = record.getStatus() != null ? record.getStatus().toUpperCase() : "PENDING";

        if (currentStatus.equals(newStatus)) {
            return Map.of("success", true, "message", "No changes made");
        }

        String vehiclePlate = getVehiclePlate(record.getVehicleId());

        return switch (newStatus) {
            case "MANAGER_APPROVED" -> {
                if (!"PENDING".equals(currentStatus))
                    throw new IllegalArgumentException("Only PENDING records can be approved by Manager");
                if (!"MANAGER".equals(actorRole) && !"ADMIN_MAKER".equals(actorRole))
                    throw new SecurityException("Only MANAGER can perform this approval");
                record.setStatus(newStatus);
                fuelRecordRepository.save(record);
                saveApprovalLog(record, "MANAGER_APPROVED", userId, vehiclePlate, meterReading, tankLevel);
                yield Map.of("success", true, "message", "Record approved by Manager — awaiting Attendant approval");
            }
            case "ATTENDANT_APPROVED" -> {
                if (!"MANAGER_APPROVED".equals(currentStatus))
                    throw new IllegalArgumentException("Record must be MANAGER_APPROVED before Attendant can approve");
                if (!"ATTENDANT".equals(actorRole) && !"ADMIN_MAKER".equals(actorRole))
                    throw new SecurityException("Only ATTENDANT can perform this approval");
                yield attendantApprove(record, id, userId, vehiclePlate, meterReading, tankLevel);
            }
            case "SIGNED" -> {
                if (!"ATTENDANT_APPROVED".equals(currentStatus))
                    throw new IllegalArgumentException("Only ATTENDANT_APPROVED records can be signed by Finance");
                if (!"FINANCE".equals(actorRole) && !"ADMIN_MAKER".equals(actorRole))
                    throw new SecurityException("Only FINANCE can sign records");
                record.setStatus(newStatus);
                fuelRecordRepository.save(record);
                saveApprovalLog(record, "SIGNED", userId, vehiclePlate, meterReading, tankLevel);
                yield Map.of("success", true, "message", "Record signed by Finance");
            }
            case "DECLINED" -> {
                if ("SIGNED".equals(currentStatus))
                    throw new IllegalArgumentException("Cannot decline a SIGNED record");
                boolean canDecline = "ADMIN_MAKER".equals(actorRole)
                        || ("PENDING".equals(currentStatus) && "MANAGER".equals(actorRole))
                        || ("MANAGER_APPROVED".equals(currentStatus) && "ATTENDANT".equals(actorRole))
                        || ("ATTENDANT_APPROVED".equals(currentStatus) && "FINANCE".equals(actorRole));
                if (!canDecline)
                    throw new SecurityException("You are not authorized to decline at this stage");
                record.setStatus(newStatus);
                fuelRecordRepository.save(record);
                saveApprovalLog(record, "DECLINED", userId, vehiclePlate, meterReading, tankLevel);
                yield Map.of("success", true, "message", "Record declined");
            }
            default -> throw new IllegalArgumentException("Invalid status");
        };
    }

    private Map<String, Object> attendantApprove(FuelRecord record, Long id, Long userId, String vehiclePlate, BigDecimal meterReading, BigDecimal tankLevel) {
        double liters = record.getLiters() != null ? record.getLiters() : 0;
        String fuelType = record.getFuelType() != null ? record.getFuelType().toLowerCase() : "";

        if (liters <= 0) throw new IllegalArgumentException("Invalid liters on record");

        Integer tankId = "diesel".equals(fuelType) ? 2 : 1;
        double[] out = new double[2];

        try {
            txTemplate.execute(st -> {
                FuelTank locked = fuelTankRepository.lockById(tankId)
                        .orElseThrow(() -> new RuntimeException(
                                fuelType + " tank (id=" + tankId + ") not found"));

                double currentLevel = getResolvedTankLevel(locked.getId());

                if (currentLevel < liters) {
                    throw new RuntimeException(
                            "Insufficient fuel. " + fuelType + " tank has " + currentLevel +
                            "L, request needs " + liters + "L");
                }

                double newLevel = currentLevel - liters;

                logFuelTransaction(locked.getId(), "OUT", liters, newLevel,
                        "APPROVAL", "Fuel record " + id + " (" + fuelType + ")", userId, meterReading, tankLevel);

                record.setStatus("ATTENDANT_APPROVED");
                fuelRecordRepository.save(record);

                out[0] = newLevel;
                out[1] = locked.getCapacity();
                return null;
            });

            saveApprovalLog(record, "ATTENDANT_APPROVED", userId, vehiclePlate, meterReading, tankLevel);

            Map<String, Object> r = new HashMap<>();
            r.put("success", true);
            r.put("message", "Approved by Attendant — " + liters + "L " + fuelType + " dispensed from tank");
            r.put("fuel_type", fuelType);
            r.put("tank_id", tankId);
            r.put("liters_deducted", liters);
            r.put("current_level", out[0]);
            r.put("percent_full", Math.round((out[0] / out[1]) * 100));
            return r;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String getVehiclePlate(Integer vehicleId) {
        if (vehicleId == null) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT number_plate FROM vehicles WHERE id = ?",
                    String.class, vehicleId);
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────
    //  DISPENSE FUEL
    // ─────────────────────────────────────────────

    public Map<String, Object> dispenseFuel(DispenseRequest req, Long userId) {
        if (req.getFuelType() == null || !req.getFuelType().equalsIgnoreCase("petrol")) {
            return Map.of("success", true, "message", "Fuel recorded (non-petrol, tank unchanged)");
        }

        double liters = req.getLiters() != null ? req.getLiters() : 0;
        double[] out = new double[3]; // [0]=newLevel, [1]=capacity, [2]=tankId

        txTemplate.execute(st -> {
            List<FuelTank> locked = fuelTankRepository.findAllWithLock();
            if (locked.isEmpty()) throw new RuntimeException("Fuel tank not found");
            FuelTank tank = locked.get(0);

            double currentLevel = getResolvedTankLevel(tank.getId());

            if (currentLevel < liters) throw new RuntimeException("Not enough fuel in tank");

            double newLevel = currentLevel - liters;

            System.out.printf("[FUEL_TANK_LOG] Dispensing: Deducting %.2fL. Previous: %.2fL → New: %.2fL%n",
                    liters, currentLevel, newLevel);

            logFuelTransaction(tank.getId(), "OUT", liters, newLevel,
                    "DISPENSE", "Direct dispense tank " + tank.getId(), userId, req.getMeterReading(), req.getTankLevel());

            out[0] = newLevel;
            out[1] = tank.getCapacity();
            out[2] = tank.getId();
            return null;
        });

        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message", "Fuel dispensed");
        r.put("id", (int) out[2]);
        r.put("capacity", out[1]);
        r.put("current_level", out[0]);
        return r;
    }

    // ─────────────────────────────────────────────
    //  REFILL TANK
    // ─────────────────────────────────────────────

    public Map<String, Object> refillTank(Integer tankId, RefillRequest req, Long userId) {
        double litersNum = req.getLiters() != null ? req.getLiters() : 0;

        if (litersNum <= 0) throw new IllegalArgumentException("Liters must be a positive number");

        double[] out = new double[2]; // [0]=newLevel, [1]=capacity

        txTemplate.execute(st -> {
            FuelTank tank = fuelTankRepository.lockById(tankId)
                    .orElseThrow(() -> new RuntimeException("Tank not found"));

            double current = getResolvedTankLevel(tank.getId());
            double capacity = tank.getCapacity();
            double newLevel = current + litersNum;

            if (newLevel > capacity) {
                throw new RuntimeException(
                        "Exceeds tank capacity. You can add at most " + (capacity - current) + "L");
            }

            System.out.printf("[FUEL_TANK_LOG] Refilling: Adding %.2fL. Previous: %.2fL → New: %.2fL%n",
                    litersNum, current, newLevel);

            logFuelTransaction(tank.getId(), "IN", litersNum, newLevel,
                    "REFILL", "Tank refill " + tank.getId(), userId, req.getMeterReading(), req.getTankLevel());

            out[0] = newLevel;
            out[1] = capacity;
            return null;
        });

        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message", "Tank refilled successfully");
        r.put("current_level", out[0]);
        r.put("capacity", out[1]);
        r.put("percent_full", Math.round((out[0] / out[1]) * 100));
        return r;
    }

    // ─────────────────────────────────────────────
    //  TANK REMOVALS REPORT
    // ─────────────────────────────────────────────

    public List<Map<String, Object>> getTankRemovals() {
        String sql = """
                SELECT
                  fr.id,
                  fr.fuel_type                          AS fuelType,
                  fr.liters,
                  fr.mileage,
                  fr.fuel_date                          AS fuelDate,
                  fr.notes,
                  COALESCE(UPPER(fr.status), 'ATTENDANT_APPROVED') AS status,
                  fr.department,
                  fr.user_name                          AS submittedBy,
                  v.number_plate                        AS vehicle,
                  al_mgr.actioned_by_username           AS approvedByManager,
                  al_att.actioned_by_username           AS approvedByAttendant,
                  al_fin.actioned_by_username           AS signedByFinance
                FROM fuel_records fr
                LEFT JOIN vehicles v ON fr.vehicle_id = v.id
                LEFT JOIN fuel_approval_log al_mgr
                       ON al_mgr.fuel_record_id = fr.id AND al_mgr.action = 'MANAGER_APPROVED'
                LEFT JOIN fuel_approval_log al_att
                       ON al_att.fuel_record_id = fr.id AND al_att.action = 'ATTENDANT_APPROVED'
                LEFT JOIN fuel_approval_log al_fin
                       ON al_fin.fuel_record_id = fr.id AND al_fin.action = 'SIGNED'
                WHERE UPPER(fr.status) IN ('ATTENDANT_APPROVED', 'SIGNED')
                ORDER BY fr.fuel_date DESC, fr.id DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }

    // ─────────────────────────────────────────────
    //  TANK REFILL HISTORY
    // ─────────────────────────────────────────────

    public List<Map<String, Object>> getTankRefills() {
        String sql = """
                SELECT
                  ft.id,
                  ft.tank_id,
                  CASE ft.tank_id WHEN 1 THEN 'petrol' WHEN 2 THEN 'diesel' ELSE 'unknown' END AS fuelType,
                  ft.litres,
                  ft.balance_after AS balanceAfter,
                  ft.reference,
                  ft.created_at   AS createdAt,
                  u.Username      AS refilledBy,
                  ft.meter_reading AS meterReading,
                  ft.tank_level   AS tankLevel
                FROM fuel_transactions ft
                LEFT JOIN users u ON ft.created_by = u.UserID
                WHERE ft.source = 'REFILL'
                ORDER BY ft.created_at DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }

    // ─────────────────────────────────────────────
    //  APPROVAL LOG
    // ─────────────────────────────────────────────

    public List<Map<String, Object>> getApprovalLog() {
        String sql = """
                SELECT
                  al.id,
                  al.fuel_record_id,
                  al.action,
                  al.actioned_by_user_id,
                  al.actioned_by_username,
                  al.actioned_by_role        AS position,
                  al.actioned_at,
                  al.vehicle,
                  al.fuel_type,
                  al.liters,
                  al.fuel_date,
                  al.meter_reading           AS meterReading,
                  al.tank_level              AS tankLevel
                FROM fuel_approval_log al
                ORDER BY al.actioned_at DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }

    // ─────────────────────────────────────────────
    //  GET TANK STATUS
    // ─────────────────────────────────────────────

    public Map<String, Object> getTank(Integer tankId) {
        FuelTank tank = fuelTankRepository.findById(tankId)
                .orElseThrow(() -> new RuntimeException("No fuel tank record found"));

        double resolvedLevel = getResolvedTankLevel(tank.getId());

        System.out.printf("[FUEL_TANK_LOG] Tank status: %.2fL / %.2fL%n", resolvedLevel, tank.getCapacity());

        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("id", tank.getId());
        r.put("capacity", tank.getCapacity());
        r.put("current_level", resolvedLevel);
        r.put("percent_full", Math.round((resolvedLevel / tank.getCapacity()) * 100));
        return r;
    }
}
