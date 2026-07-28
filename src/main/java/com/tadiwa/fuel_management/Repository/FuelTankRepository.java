package com.tadiwa.fuel_management.Repository;

import com.tadiwa.fuel_management.Entity.FuelTank;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelTankRepository extends JpaRepository<FuelTank, Integer> {

    Optional<FuelTank> findFirstByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM FuelTank t ORDER BY t.id ASC")
    List<FuelTank> findAllWithLock();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM FuelTank t WHERE t.id = :id")
    Optional<FuelTank> lockById(@Param("id") Integer id);
}
