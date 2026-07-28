package com.tadiwa.fuel_management.Repository;

import com.tadiwa.fuel_management.Entity.FuelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FuelRecordRepository extends JpaRepository<FuelRecord, Long> {
}
