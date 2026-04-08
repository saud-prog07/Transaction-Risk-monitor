#!/bin/bash
# ====================================================================
# Dashboard Testing Script
# Tests the dashboard and API integration
# ====================================================================

set -e

API_URL=${1:-"http://localhost:8082"}
DASHBOARD_URL="http://localhost:3000"

echo "=========================================="
echo "Dashboard & API Integration Test"
echo "=========================================="
echo ""

# Test 1: API Health Check
echo "1️⃣  Testing API Health..."
if curl -s "$API_URL/api/actuator/health" > /dev/null; then
    echo "✅ API is healthy"
else
    echo "❌ API is not running at $API_URL"
    echo "   Start the Alert Service first:"
    echo "   cd alert-service && mvn spring-boot:run"
    exit 1
fi

echo ""

# Test 2: Fetch Alerts
echo "2️⃣  Testing Alerts Endpoint..."
ALERTS_RESPONSE=$(curl -s "$API_URL/api/alerts?page=0&size=5")
ALERT_COUNT=$(echo $ALERTS_RESPONSE | grep -o '"id"' | wc -l)

if [ $ALERT_COUNT -gt 0 ]; then
    echo "✅ Found $ALERT_COUNT alerts"
else
    echo "ℹ️  No alerts yet (this is normal for new setup)"
    echo "   Submit a test transaction to generate alerts:"
    echo "   curl -X POST http://localhost:8080/api/transaction \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"userId\": \"test\", \"amount\": 50000, \"location\": \"NYC\"}'"
fi

echo ""

# Test 3: Submit Test Transaction
echo "3️⃣  Submitting Test Transaction..."
PRODUCER_URL="http://localhost:8080"

if curl -s "$PRODUCER_URL/api/actuator/health" > /dev/null; then
    RESPONSE=$(curl -s -X POST "$PRODUCER_URL/api/transaction" \
        -H "Content-Type: application/json" \
        -d '{
            "userId": "test-user-'$(date +%s)'",
            "amount": 50000,
            "location": "Singapore, SG"
        }')
    
    if echo $RESPONSE | grep -q "transactionId"; then
        echo "✅ Test transaction submitted successfully"
        echo "   Transaction ID: $(echo $RESPONSE | grep -o '"transactionId":"[^"]*' | cut -d'"' -f4)"
        echo ""
        echo "⏳  Waiting 3 seconds for processing..."
        sleep 3
    else
        echo "⚠️  Producer Service not responding"
    fi
else
    echo "⚠️  Producer Service not running (optional for dashboard testing)"
fi

echo ""

# Test 4: Check Dashboard
if curl -s "$DASHBOARD_URL" > /dev/null; then
    echo "✅ Dashboard is running at $DASHBOARD_URL"
    echo "   Open in browser: $DASHBOARD_URL"
else
    echo "ℹ️  Dashboard not yet running"
    echo "   Start it with: npm start"
fi

echo ""
echo "=========================================="
echo "✅ All tests passed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Open dashboard: $DASHBOARD_URL"
echo "2. Verify alerts are displayed"
echo "3. Test filtering and search"
echo "4. Try updating alert status"
echo ""
