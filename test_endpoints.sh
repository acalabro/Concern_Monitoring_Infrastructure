#!/bin/bash

echo "=== TEST ENDPOINT BACKEND ==="
echo ""

echo "Testing http://localhost:8181/api/health"
curl -s http://localhost:8181/api/health | jq '.' 2>/dev/null || echo "FAILED"
echo ""

echo "Testing http://localhost:8181/api/status"
curl -s http://localhost:8181/api/status | jq '.' 2>/dev/null || echo "FAILED"
echo ""

echo "Testing http://localhost:8181/api/metrics"
curl -s http://localhost:8181/api/metrics | jq '.' 2>/dev/null || echo "FAILED"
echo ""

echo "Testing http://localhost:8181/api/rules"
curl -s http://localhost:8181/api/rules | jq '.' 2>/dev/null || echo "FAILED"
echo ""

echo "Testing http://localhost:8181/api/stats/events"
curl -s http://localhost:8181/api/stats/events | jq '.' 2>/dev/null || echo "FAILED"
echo ""

echo "Testing http://localhost:8181/api/stats/violations"
curl -s http://localhost:8181/api/stats/violations | jq '.' 2>/dev/null || echo "FAILED"
echo ""

echo "=== ENDPOINT RULES MANAGEMENT API ==="
echo ""

echo "Testing http://localhost:8181/api/rules/list"
curl -s http://localhost:8181/api/rules/list | jq '.' 2>/dev/null || echo "FAILED"
echo ""

echo "Testing http://localhost:8181/api/rules/validate (POST)"
curl -s -X POST http://localhost:8181/api/rules/validate \
  -H "Content-Type: application/json" \
  -d '{"ruleContent":"package test; rule \"test\" when then end"}' \
  | jq '.' 2>/dev/null || echo "FAILED"
echo ""

echo "=== FINE TEST ==="
