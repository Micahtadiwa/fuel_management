-- Verify Refill Operations

-- 1. Check fuel_tank table structure and current tanks
SELECT 'FUEL TANKS:' AS section;
DESC fuel_tank;
SELECT * FROM fuel_tank;
SELECT '' AS '';

-- 2. Check fuel_transactions for refill operations
SELECT 'REFILL TRANSACTIONS (last 10):' AS section;
SELECT
  id,
  tank_id,
  movement_type,
  litres,
  balance_after,
  source,
  reference,
  meter_reading,
  tank_level,
  created_at,
  created_by
FROM fuel_transactions
WHERE source = 'REFILL'
ORDER BY created_at DESC
LIMIT 10;
SELECT '' AS '';

-- 3. Check latest transaction per tank (current level)
SELECT 'CURRENT TANK LEVELS (from latest transactions):' AS section;
SELECT
  ft.tank_id,
  CASE ft.tank_id WHEN 1 THEN 'Petrol' WHEN 2 THEN 'Diesel' ELSE 'Unknown' END AS fuel_type,
  fuelTank.capacity,
  MAX(ft.balance_after) AS current_level,
  ROUND((MAX(ft.balance_after) / fuelTank.capacity) * 100, 2) AS percent_full,
  ft.id AS latest_transaction_id,
  ft.created_at,
  ft.source
FROM fuel_transactions ft
JOIN fuel_tank fuelTank ON ft.tank_id = fuelTank.id
WHERE (ft.tank_id, ft.created_at) IN (
  SELECT tank_id, MAX(created_at)
  FROM fuel_transactions
  GROUP BY tank_id
)
GROUP BY ft.tank_id, fuelTank.capacity, fuelTank.id;
SELECT '' AS '';

-- 4. Check all IN/OUT transactions for tank 1
SELECT 'TANK 1 TRANSACTION HISTORY (IN/OUT):' AS section;
SELECT
  id,
  movement_type,
  litres,
  balance_after,
  source,
  meter_reading,
  tank_level,
  created_at
FROM fuel_transactions
WHERE tank_id = 1
ORDER BY created_at ASC;
