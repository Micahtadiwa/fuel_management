-- Add meter_reading column to fuel_approval_log table
-- Purpose: Allow authorizers to record meter readings when approving fuel

ALTER TABLE fuel_approval_log
ADD COLUMN meter_reading DECIMAL(10, 2) NULL
COMMENT 'Optional meter reading (e.g., fuel tank gauge) recorded by authorizer during approval';

-- Add meter_reading column to fuel_transactions table
-- Purpose: Record meter readings for all fuel transactions (refills, dispenses, approvals)

ALTER TABLE fuel_transactions
ADD COLUMN meter_reading DECIMAL(10, 2) NULL
COMMENT 'Optional meter reading recorded during fuel transaction';
