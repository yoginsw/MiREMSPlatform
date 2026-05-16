#!/usr/bin/env python3
"""Structural verification for P10 production-hardening assets."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = [
    "infrastructure/k8s/helm/mirems-platform/Chart.yaml",
    "infrastructure/k8s/helm/mirems-platform/values.yaml",
    "infrastructure/k8s/helm/mirems-platform/templates/core-api-deployment.yaml",
    "infrastructure/k8s/helm/mirems-platform/templates/core-api-service.yaml",
    "infrastructure/k8s/helm/mirems-platform/templates/ingress.yaml",
    "infrastructure/k8s/helm/mirems-platform/templates/networkpolicy.yaml",
    "infrastructure/k8s/helm/mirems-platform/templates/secret.yaml",
    "infrastructure/k8s/helm/mirems-platform/templates/serviceaccount.yaml",
    "infrastructure/security/trivy-scan.sh",
    "infrastructure/security/zap-api-scan.sh",
    "infrastructure/security/.trivyignore",
    "infrastructure/grafana/dashboards/mirems-platform.json",
    "docs/runbooks/production-operations.md",
    "docs/runbooks/backup-restore.md",
    "docs/vvsg/VVSG2_FINAL_COMPLIANCE_REPORT.md",
]


def read(relative: str) -> str:
    path = ROOT / relative
    assert path.exists(), f"missing required P10 asset: {relative}"
    return path.read_text(encoding="utf-8")


def test_required_assets_exist() -> None:
    for relative in REQUIRED_FILES:
        assert (ROOT / relative).exists(), relative


def test_helm_chart_has_production_hardening_controls() -> None:
    values = read("infrastructure/k8s/helm/mirems-platform/values.yaml")
    deployment = read("infrastructure/k8s/helm/mirems-platform/templates/core-api-deployment.yaml")
    network_policy = read("infrastructure/k8s/helm/mirems-platform/templates/networkpolicy.yaml")
    secret = read("infrastructure/k8s/helm/mirems-platform/templates/secret.yaml")

    for marker in ["readinessProbe", "livenessProbe", "resources:", "securityContext:", "runAsNonRoot: true"]:
        assert marker in deployment, marker
    for marker in ["MIREMS_DB_URL", "MIREMS_KEYCLOAK_URL", "MIREMS_KAFKA_BOOTSTRAP"]:
        assert marker in deployment, marker
    assert "kind: NetworkPolicy" in network_policy
    assert "ingress:" in network_policy and "egress:" in network_policy
    assert "port: 53" in network_policy and "UDP" in network_policy
    assert "existingSecret" in values
    assert "[REDACTED]" not in values
    assert "stringData:" in secret
    assert "if not .Values.coreApi.secrets.existingSecret" in secret


def test_security_scan_scripts_are_safe_and_actionable() -> None:
    trivy = read("infrastructure/security/trivy-scan.sh")
    zap = read("infrastructure/security/zap-api-scan.sh")
    for script in [trivy, zap]:
        assert "set -euo pipefail" in script
        assert "[REDACTED]" not in script
    assert "trivy image" in trivy
    assert "--exit-code 1" in trivy
    assert "zap-api-scan.py" in zap
    assert "docs/api/mirems-api.yaml" in zap
    assert " -I" not in zap


def test_grafana_dashboard_json_is_valid_and_has_election_panels() -> None:
    dashboard_path = ROOT / "infrastructure/grafana/dashboards/mirems-platform.json"
    dashboard = json.loads(dashboard_path.read_text(encoding="utf-8"))
    titles = {panel.get("title", "") for panel in dashboard.get("panels", [])}
    assert "HTTP request rate" in titles
    assert "API latency p95" in titles
    assert "Audit events by type" in titles
    assert "Voting sessions opened" in titles


def test_runbooks_and_final_vvsg_report_have_required_sections() -> None:
    ops = read("docs/runbooks/production-operations.md")
    backup = read("docs/runbooks/backup-restore.md")
    report = read("docs/vvsg/VVSG2_FINAL_COMPLIANCE_REPORT.md")
    for marker in ["Helm deployment", "Security scanning", "Incident response", "Rollback"]:
        assert marker in ops, marker
    for marker in ["Backup schedule", "Restore procedure", "Integrity verification", "RPO", "RTO"]:
        assert marker in backup, marker
    for marker in ["Principle 1", "Principle 7", "Principle 10", "Evidence inventory", "Open items"]:
        assert marker in report, marker
