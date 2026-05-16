#!/usr/bin/env bash
set -euo pipefail

IMAGE_REF="${1:-${MIREMS_IMAGE_REF:-ghcr.io/yoginsw/mirems-core-api:0.1.0-SNAPSHOT}}"
REPORT_DIR="${MIREMS_SECURITY_REPORT_DIR:-build/security}"
mkdir -p "${REPORT_DIR}"

trivy image \
  --severity HIGH,CRITICAL \
  --ignore-unfixed \
  --exit-code 1 \
  --format sarif \
  --output "${REPORT_DIR}/trivy-image.sarif" \
  "${IMAGE_REF}"

trivy image \
  --severity HIGH,CRITICAL \
  --ignore-unfixed \
  --exit-code 0 \
  --format table \
  "${IMAGE_REF}" | tee "${REPORT_DIR}/trivy-image.txt"
