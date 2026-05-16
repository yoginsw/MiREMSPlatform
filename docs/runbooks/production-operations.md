# MiREMS Production Operations Runbook

## Scope

This runbook covers the production operation of the MiREMS Platform Kubernetes deployment. It assumes a managed PostgreSQL 16.4 database, Kafka-compatible broker, Keycloak realm, TLS-enabled ingress, and external secret management. Never store production credentials in Git; use sealed secrets or the cluster secret manager and keep examples redacted.

## Helm deployment

1. Build and sign release images in CI.
2. Run container and API security scans before promotion.
3. Create a production values file outside the repository with real endpoint values and `coreApi.secrets.existingSecret` pointing to a pre-created Secret from the cluster secret manager or sealed-secrets controller.
4. Deploy or upgrade:

```bash
helm upgrade --install mirems-platform infrastructure/k8s/helm/mirems-platform   --namespace mirems --create-namespace   --values values.production.yaml   --set coreApi.image.tag=${RELEASE_TAG}
```

5. Verify rollout:

```bash
kubectl -n mirems rollout status deployment/mirems-platform-mirems-platform-core-api
kubectl -n mirems get pods,svc,ingress
kubectl -n mirems logs deploy/mirems-platform-mirems-platform-core-api --tail=100
```

## Security scanning

Run these before production promotion and archive outputs under the release evidence folder:

```bash
infrastructure/security/trivy-scan.sh ghcr.io/yoginsw/mirems-core-api:${RELEASE_TAG}
MIREMS_ZAP_TARGET_URL=https://mirems.example.gov/miremsplatform infrastructure/security/zap-api-scan.sh
```

High or critical findings block release unless a documented risk acceptance is approved by the election security authority.

## Monitoring

Import `infrastructure/grafana/dashboards/mirems-platform.json` into Grafana. Alert on:

- API availability below 99.9% over 10 minutes.
- p95 latency above 1.5 seconds for critical election APIs.
- Any sustained 5xx response rate above 0.5%.
- Audit event throughput unexpectedly dropping to zero during active election operations.

## Incident response

1. Declare severity and incident commander.
2. Preserve evidence: pod logs, audit events, Kubernetes events, image digest, Helm release metadata.
3. If vote data or audit integrity may be affected, freeze destructive operations and notify election officials.
4. Apply containment: scale down affected component, revoke tokens, or isolate namespace using NetworkPolicy.
5. Record timeline, root cause, remediation, and VVSG evidence impact.

## Rollback

Use rollback only when forward fix is riskier than returning to a verified previous release:

```bash
helm -n mirems history mirems-platform
helm -n mirems rollback mirems-platform <REVISION>
kubectl -n mirems rollout status deployment/mirems-platform-mirems-platform-core-api
```

After rollback, verify health endpoints, login, election list, voting session smoke path, and audit event creation.

## Release evidence checklist

- Image digest and SBOM.
- Trivy report.
- ZAP report.
- Helm values checksum with secrets redacted.
- Playwright/k6 evidence for the target environment.
- Database backup snapshot ID before deployment.
- VVSG final compliance report revision.
