package com.tadiwa.fuel_management.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        ensureColumnsExist();
    }

    private void ensureColumnsExist() {
        try {
            // Check and add tank_level to fuel_transactions if it doesn't exist
            if (!columnExists("fuel_transactions", "tank_level")) {
                jdbcTemplate.execute(
                    "ALTER TABLE fuel_transactions " +
                    "ADD COLUMN tank_level DECIMAL(10, 2) NULL " +
                    "COMMENT 'Optional tank level recorded during fuel transaction'"
                );
                System.out.println("✓ Added tank_level column to fuel_transactions");
            }

            // Check and add tank_level to fuel_approval_log if it doesn't exist
            if (!columnExists("fuel_approval_log", "tank_level")) {
                jdbcTemplate.execute(
                    "ALTER TABLE fuel_approval_log " +
                    "ADD COLUMN tank_level DECIMAL(10, 2) NULL " +
                    "COMMENT 'Optional tank level recorded by authorizer during approval'"
                );
                System.out.println("✓ Added tank_level column to fuel_approval_log");
            }

            // Check and add meter_reading to fuel_transactions if it doesn't exist
            if (!columnExists("fuel_transactions", "meter_reading")) {
                jdbcTemplate.execute(
                    "ALTER TABLE fuel_transactions " +
                    "ADD COLUMN meter_reading DECIMAL(10, 2) NULL " +
                    "COMMENT 'Optional meter reading recorded during fuel transaction'"
                );
                System.out.println("✓ Added meter_reading column to fuel_transactions");
            }

            // Check and add meter_reading to fuel_approval_log if it doesn't exist
            if (!columnExists("fuel_approval_log", "meter_reading")) {
                jdbcTemplate.execute(
                    "ALTER TABLE fuel_approval_log " +
                    "ADD COLUMN meter_reading DECIMAL(10, 2) NULL " +
                    "COMMENT 'Optional meter reading recorded by authorizer during approval'"
                );
                System.out.println("✓ Added meter_reading column to fuel_approval_log");
            }

        } catch (Exception e) {
            System.err.println("⚠ Database initialization error: " + e.getMessage());
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
