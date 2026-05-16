# MiREMS Backup and Restore Runbook

## Scope

This runbook covers backup and restore procedures for MiREMS production data: PostgreSQL election records, Keycloak realm data, Kafka audit/event streams, and release evidence artifacts.

## Backup schedule

- PostgreSQL: continuous WAL archiving plus full logical backup every 24 hours.
- Keycloak realm: nightly export and pre-release export.
- Kafka: broker-level replicated storage snapshots and topic retention aligned with election law.
- Evidence artifacts: immutable object storage retention for release, audit, scan, and VVSG reports.

## RPO

Target recovery point objective: 15 minutes for transactional data during active election periods. During non-election periods, RPO may be extended to 24 hours with written approval.

## RTO

Target recovery time objective: 4 hours for core administrative functions and 8 hours for full reporting and evidence services.

## PostgreSQL backup procedure

```bash
kubectl -n mirems create job --from=cronjob/mirems-postgres-backup manual-backup-$(date +%Y%m%d%H%M)
```

For managed databases, trigger the provider snapshot and record the snapshot identifier in the release evidence folder.

## Restore procedure

1. Open an incident/change ticket and assign a restore commander.
2. Stop write traffic to the application namespace.
3. Restore PostgreSQL to an isolated validation database first.
4. Run Flyway validation and domain smoke tests.
5. Restore Keycloak realm export matching the application release.
6. Reconnect Kafka/event replay according to approved election authority guidance.
7. Switch application configuration to the restored endpoints.
8. Run verification: health, login, election list, voting-session smoke, tabulation report, and audit export.

## Integrity verification

- Compare restored row counts for election, ballot, voting result, correction, and audit tables against backup manifest.
- Validate immutable vote-record hashes and tabulation-report hashes.
- Verify audit chain-of-custody continuity before and after the restore point.
- Confirm no credentials or raw PII are written to restore logs.

## Evidence retention

Store backup manifests, restore command logs with secrets redacted, validation outputs, and official approvals in immutable evidence storage.
