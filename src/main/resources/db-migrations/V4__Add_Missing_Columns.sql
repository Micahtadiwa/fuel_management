-- Ensure tank_level column exists in fuel_transactions
ALTER TABLE fuel_transactions
ADD COLUMN tank_level DECIMAL(10, 2) NULL
COMMENT 'Optional tank level recorded during fuel transaction';

-- Ensure tank_level column exists in fuel_approval_log
ALTER TABLE fuel_approval_log
ADD COLUMN tank_level DECIMAL(10, 2) NULL
COMMENT 'Optional tank level recorded by authorizer during approval';
