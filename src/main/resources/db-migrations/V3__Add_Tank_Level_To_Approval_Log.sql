-- Add tank_level column to fuel_approval_log table
-- Purpose: Allow authorizers to record tank level when approving fuel

ALTER TABLE fuel_approval_log
ADD COLUMN tank_level DECIMAL(10, 2) NULL
COMMENT 'Optional tank level recorded by authorizer during approval';
