# ADR-002: Hybrid BPMN/DMN and Direct Service Architecture

**Status:** Accepted

**Date:** 2026-05-14

## Context

Election systems contain both simple transactional operations and long-running, multi-party, auditable workflows. Treating every operation as a workflow would create unnecessary complexity. Treating every operation as a direct service would hide important process steps that must be inspectable and auditable.

MiREMS must support explicit election workflows such as election publication, candidate registration, ballot tabulation, vote correction, result certification, and audit review. It must also support fast direct operations such as simple CRUD, lookup, and stateless calculations.

## Decision

Use a hybrid architecture:

- Use Kogito BPMN for long-running, multi-party, human-reviewed, or explicitly auditable workflows.
- Use Kogito DMN for declarative business rules and eligibility decisions.
- Use Direct Spring Services for simple CRUD, stateless queries, and single-transaction business operations.
- Direct Services must never call Kogito APIs directly.
- Any Direct Service that starts or signals a process must use the `MiremsProcessService` wrapper interface.
- The Kogito-specific implementation lives in `core-bpmn`; service and API layers depend on the abstraction, not on Kogito internals.

Decision rule:

```text
Is the operation a long-running, multi-party, auditable workflow?
  YES -> BPMN process via MiremsProcessService
  NO  -> Is it a stateless business rule or policy decision?
          YES -> DMN decision
          NO  -> Direct Spring Service
```

## Consequences

Positive consequences:

- Critical election procedures are visible as inspectable process models.
- Direct Services remain simple and testable.
- Kogito can be mocked or replaced behind `MiremsProcessService`.
- Auditability is improved for workflows that involve approvals, timers, or failure paths.

Negative consequences:

- Developers must consistently apply the BPMN/DMN/Direct decision rule.
- BPMN integration tests require more setup than pure unit tests.
- Process definitions and service handlers must remain synchronized.
- Incorrectly choosing Direct Service for a process-heavy feature can weaken auditability.

## Compliance Notes

Workflows involving publication, tabulation, correction, certification, and audit review must preserve process-instance evidence sufficient to reconstruct who acted, when, and why a transition occurred.
