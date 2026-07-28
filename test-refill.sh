#!/bin/bash

# Test script to verify refill adds fuel to tank

API="http://localhost:8080/api"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Fuel Tank Refill Test ===${NC}\n"

# 1. Get current tank status
echo -e "${BLUE}1. Getting current tank status (Tank ID 1 - Petrol):${NC}"
curl -s -X GET "$API/fuel/tank/1" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq '.'
echo ""

# 2. Refill the tank with 500 liters
echo -e "${BLUE}2. Refilling tank with 500 liters, meter reading 45000.50, tank level 95.5%:${NC}"
REFILL_RESPONSE=$(curl -s -X POST "$API/fuel/tank/1/refill" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "liters": 500,
    "meterReading": 45000.50,
    "tankLevel": 95.5
  }')
echo "$REFILL_RESPONSE" | jq '.'
echo ""

# 3. Check tank status again
echo -e "${BLUE}3. Checking tank status after refill:${NC}"
curl -s -X GET "$API/fuel/tank/1" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq '.'
echo ""

# 4. Get tank refill history
echo -e "${BLUE}4. Checking tank refill history (should show new refill):${NC}"
curl -s -X GET "$API/fuel/tank-refills" \
  -H "Authorization: Bearer YOUR_TOKEN" | jq '.'
echo ""

echo -e "${GREEN}✓ Test complete${NC}"
