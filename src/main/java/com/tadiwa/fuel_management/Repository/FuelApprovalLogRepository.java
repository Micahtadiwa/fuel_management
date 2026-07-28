package com.tadiwa.fuel_management.Repository;

import com.tadiwa.fuel_management.Entity.FuelApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuelApprovalLogRepository extends JpaRepository<FuelApprovalLog, Long> {
    List<FuelApprovalLog> findByFuelRecordIdOrderByActionedAtDesc(Long fuelRecordId);
}
