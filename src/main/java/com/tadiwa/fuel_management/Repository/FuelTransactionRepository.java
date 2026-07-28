package com.tadiwa.fuel_management.Repository;


import com.tadiwa.fuel_management.Entity.FuelTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuelTransactionRepository extends JpaRepository<FuelTransaction, Long> {

    @Query("SELECT ft FROM FuelTransaction ft WHERE ft.fuelTank.id = :tankId ORDER BY ft.createdAt DESC, ft.id DESC")
    List<FuelTransaction> findLatestByTankId(@Param("tankId") Integer tankId, Pageable pageable);
}
