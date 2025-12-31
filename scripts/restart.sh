#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${HOME}/apps/oms-fix-platform"
OMS_LOG="${HOME}/apps/logs/oms-service.log"
FIX_LOG="${HOME}/apps/logs/fix-gateway.log"

echo "[restart] oms+fix -> ${APP_HOME}"

mkdir -p "$(dirname "$OMS_LOG")"
cd "$APP_HOME"

# stop old instances
pkill -f "oms-service.*bootRun" || true
pkill -f "fix-gateway.*bootRun" || true
pkill -f "com.example.oms.OmsServiceApplication" || true
pkill -f "com.example.fix.FixGatewayApplication" || true

# start OMS
nohup ./gradlew :oms-service:bootRun >"$OMS_LOG" 2>&1 &
sleep 2

# start FIX gateway
nohup ./gradlew :fix-gateway:bootRun >"$FIX_LOG" 2>&1 &

echo "[restart] oms started (log: $OMS_LOG)"
echo "[restart] fix started (log: $FIX_LOG)"
