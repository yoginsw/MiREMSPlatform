# ADR-006: Append-Only Audit Log Design

**Status:** Accepted

**Date:** 2026-05-14

## Context

MiREMS must be able to reconstruct all important state changes affecting elections, ballots, candidates, vote records, tabulation, certification, security, and administrative actions. Audit logs must be complete, queryable, and resistant to accidental or unauthorized deletion.

The platform uses Spring services, a PostgreSQL primary store, and Kafka for event streaming. State changes should produce durable audit records without relying on manual logging scattered across controllers.

## Decision

Use an append-only audit architecture based on domain events.

- Domain aggregates emit domain events for meaningful state changes.
- `AuditEventPublisher` persists each event to an append-only `AuditEvent` table and publishes the same event to Kafka topic `mirems.audit.events`.
- Audit persistence must occur after successful transaction commit where applicable.
- `@AuditAction` provides service-level AOP for state-changing operations that require audit emission.
- Audit records include event type, aggregate ID, aggregate type, payload, actor ID, occurred timestamp, and source IP where available.
- Audit repositories expose save and read-only query methods only; no delete method is provided.
- Audit APIs are restricted to `AUDITOR` and `SYSTEM_ADMIN` roles unless a narrower role is explicitly approved.

## Consequences

Positive consequences:

- Auditors can query a canonical event trail.
- Kafka enables downstream monitoring, reporting, and external audit integrations.
- Transaction-after-commit publication reduces mismatch between database state and audit events.
- AOP reduces the risk of missing audit calls in routine service methods.

Negative consequences:

- Event schemas must be versioned carefully.
- Kafka outages require defined retry or outbox behavior in later hardening phases.
- Audit payloads must avoid leaking raw PII.
- Append-only storage requires retention, archival, and backup planning.

## Compliance Notes

Every state change to `Election`, `Ballot`, `Candidate`, `VotingResult`, tabulation reports, certification, and security-relevant events must be auditable. Audit payloads must protect PII while preserving enough evidence for VVSG-aligned verification and investigation.
