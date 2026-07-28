-- Add tank_level column to fuel_transactions table
-- Purpose: Record optional tank level during fuel transactions (refills, dispenses, approvals)

ALTER TABLE fuel_transactions
ADD COLUMN tank_level DECIMAL(10, 2) NULL
COMMENT 'Optional tank level (percentage or actual gauge reading) recorded during fuel transaction';
