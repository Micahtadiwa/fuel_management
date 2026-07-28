package com.tadiwa.fuel_management.Repository;

import com.tadiwa.fuel_management.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    java.util.List<User> findByFailedLoginAttemptsGreaterThanEqual(int attempts);
}