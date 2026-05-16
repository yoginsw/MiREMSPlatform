#!/usr/bin/env bash
set -euo pipefail

TARGET_URL="${MIREMS_ZAP_TARGET_URL:-http://localhost:8080/miremsplatform}"
OPENAPI_SPEC="${MIREMS_OPENAPI_SPEC:-docs/api/mirems-api.yaml}"
REPORT_DIR="${MIREMS_SECURITY_REPORT_DIR:-build/security}"
mkdir -p "${REPORT_DIR}"

zap-api-scan.py \
  -t "${OPENAPI_SPEC}" \
  -f openapi \
  -O "${TARGET_URL}" \
  -r "${REPORT_DIR}/zap-api-report.html" \
  -J "${REPORT_DIR}/zap-api-report.json" \
  -w "${REPORT_DIR}/zap-api-report.md"
