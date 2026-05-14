# MiREMS Platform — VVSG 2.0 Compliance Mapping

**Document status:** Initial traceability baseline  
**Project version:** 0.1.0-SNAPSHOT  
**Source standard:** NIST/EAC Voluntary Voting System Guidelines 2.0  
**Primary source material:** `_temp/VVSG_2.0_Guidelines.md`, `_temp/VVSG_2.0_Test_Assertions_v1.4.md`  
**Last updated:** 2026-05-14

> This document is a planning and traceability matrix. It does **not** assert certification. Until implementation, tests, operational procedures, and VSTL evidence exist, MiREMS should be described as **VVSG 2.0-aligned / designed for VVSG 2.0 traceability**, not as certified or fully compliant.

---

## 1. Status Legend

- `IMPLEMENTED` — implemented in code and verified by tests/evidence.
- `PLANNED` — in project scope and mapped to a module/goal, but not yet implemented.
- `DEFERRED` — potentially applicable but intentionally scheduled for a later phase or external operational package.
- `N/A` — not applicable to the MiREMS software scope, usually because the requirement applies to physical devices, paper handling hardware, environmental tests, or jurisdiction-specific operational procedures outside this platform.

Current baseline: because MiREMS is at scaffold stage, nearly all applicable requirements are `PLANNED` or `DEFERRED`; none are marked `IMPLEMENTED` yet.

---

## 2. MiREMS Module Reference

- `core-domain` — Election, Contest, Candidate, Ballot, BallotStyle, VoterRecord, VotingSession, VotingResult, AuditEvent, ExtensionPack interfaces.
- `core-bpmn` — Kogito BPMN/DMN processes: election publication, candidate registration, voter eligibility, ballot tabulation, vote correction, result certification, audit review.
- `core-api` — REST controllers, OpenAPI contract, generated server stubs, RFC 7807 errors, security annotations.
- `core-infra` — JPA, Flyway, PostgreSQL, Kafka, encryption converters, audit publisher, logging, observability.
- `mirems-auth` — Keycloak/OIDC integration, RBAC helpers, election-scoped authorization.
- `frontend/mirems-shell` — React shell, routing, auth, admin UI, voting kiosk UI.
- `frontend/packages/ui-core` — accessible reusable UI components.
- `frontend/packages/api-client` — generated TypeScript API client.
- `extensions/ext-kr`, `extensions/ext-us` — country-specific legal/rule packs.
- `docs/adr` — architectural decisions.
- `docs/api` — OpenAPI 3.1 contract.
- `docs/extensions` — country/legal references.
- `infrastructure` — Docker, Keycloak realm, Kafka, PostgreSQL, Kubernetes/Helm.

---

## 3. Volume I Functional Coverage Summary

This section covers the VVSG 2.0 functional categories represented by Principles 1–15 in the source index.

### Principle 1 — High Quality Design

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `1.1.1-*` Election definition
- `1.1.2-*` Pre-election testing and readiness
- `1.1.3-*` Opening polls
- `1.1.4-*` Voting and casting ballot variations
- `1.1.5-*` Casting and recording
- `1.1.6-*` Ballot handling for vote-capture devices
- `1.1.7-*` Exiting/suspending election mode
- `1.1.8-*` Tabulation
- `1.1.9-*` Post-election reports
- `1.2-*` Accuracy, reliability, stress, failure handling
- `1.3-*` Manufacturer/performance testing evidence

**MiREMS mapping:**

- Election definition: `core-domain` Election, Contest, Candidate, Ballot, BallotStyle; `core-api`; `docs/api/mirems-api.yaml`.
- Pre-election readiness: `ElectionPublicationProcess.bpmn`; future logic and accuracy checklist APIs.
- Voting/casting: `VotingSessionService`, `VotingSessionPage`, `VotingResult`.
- Recording: immutable `VotingResult`, SHA-256 hash, audit events.
- Tabulation: `BallotTabulationProcess.bpmn`, `TabulationReportService`, extension-specific tabulation rules.
- Reports: `TabulationReport`, results dashboard, certified result export.

**Notes:** Hardware scanner/paper-feed requirements in `1.1.6-*` are `N/A` unless MiREMS later controls physical scanners or ballot marking devices.

---

### Principle 2 — High Quality Implementation

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `2.1-*` Software quality, coding conventions, record retention
- `2.2-*` User-centered design process
- `2.3-*` Clear logic, no hardcoded secrets
- `2.4-*` Modularity and testability
- `2.5-*` Data/process integrity, input validation, injection prevention
- `2.6-*` Error recovery and data protection during failures
- `2.7-*` Reliability and environmental resilience

**MiREMS mapping:**

- Java/TypeScript standards: `AGENTS.md`, Gradle, pnpm, ESLint/Prettier.
- Modularity: `mirems-core` split into domain, BPMN, API, infra; Extension Pack isolation.
- Input validation: Jakarta Validation, OpenAPI schema, TypeScript types.
- SQL injection prevention: Spring Data JPA/repositories and parameterized queries.
- Secrets: environment variables only; no hardcoded secrets.
- Testing: JUnit, Mockito, AssertJ, Testcontainers, Vitest, Playwright.
- Failure handling: transaction boundaries, append-only audit, future outbox/retry design.

**Notes:** Environmental hardware tests are `N/A` for hosted software unless MiREMS is bundled with dedicated voting devices.

---

### Principle 3 — Transparent

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `3.1.1-*` System overview documentation
- `3.1.2-*` Performance documentation
- `3.1.3-*` Security and audit documentation
- `3.1.4-*` Software installation documentation
- `3.1.5-*` Operations documentation
- `3.1.6-*` Maintenance documentation
- `3.1.7-*` Training documentation
- `3.2-*` Setup inspection process
- `3.3-*` Event logging and common data format documentation

**MiREMS mapping:**

- System docs: `README.md`, `AGENTS.md`, `PLAN.md`, `docs/adr`.
- API docs: `docs/api/mirems-api.yaml`, SpringDoc.
- Security docs: `docs/auth/ROLE_MATRIX.md`, Keycloak realm export.
- Audit docs: `ADR-006-audit-log-design.md`, audit API docs.
- Install docs: future deployment/runbook docs under `docs/runbook` and `infrastructure`.

**Notes:** Training/operations manuals are `DEFERRED` to production hardening and deployment packaging.

---

### Principle 4 — Interoperable

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `4.1-*` Election programming, tabulator reports, CVR exchange, event logs
- `4.2-*` Standard formats
- `4.3-*` Standard device interfaces
- `4.4-*` COTS device requirements

**MiREMS mapping:**

- OpenAPI 3.1: `docs/api/mirems-api.yaml`.
- Audit/event export: Kafka topic `mirems.audit.events`, audit JSON export.
- CVR/result export: `VotingResult`, `TabulationReport`, future JSON/XML export.
- Common data format compatibility: `PLANNED`; evaluate NIST election common data formats in P3/P8.

**Notes:** Standard physical device interfaces and COTS hardware requirements are `N/A` unless MiREMS integrates with physical voting devices.

---

### Principle 5 — Equivalent and Consistent Voter Access

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `5.1-A` Voting methods and interaction modes
- `5.1-B` Languages
- `5.1-C` Vote records
- `5.1-D` Accessibility features
- `5.1-E` Reading paper ballots
- `5.1-F` Accessibility documentation
- `5.2-*` Equivalent presentation across languages and modes

**MiREMS mapping:**

- Voting methods: `VotingSession`, `VotingSessionPage`, extension-specific voting methods.
- Languages: `frontend/packages/i18n` with English/Korean baseline and extension locales.
- Vote records: immutable `VotingResult` in `core-domain` and `core-infra`.
- Accessibility features: `ui-core`, kiosk high-contrast/keyboard/screen-reader support.

**Notes:** Paper ballot reading is `N/A` unless scanner/paper processing is added.

---

### Principle 6 — Voter Privacy

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `6.1-*` Preserving privacy during activation, marking, verification, casting
- `6.2-A` Voter independence

**MiREMS mapping:**

- PII protection: encrypted `VoterRecord.externalVoterId`.
- Vote secrecy: separate voter/session identity handling from ballot selections where possible.
- DTO masking: response DTOs must not expose raw voter identifiers.
- Kiosk UI: accessibility and independent operation support.

**Notes:** Physical privacy screens, audio jack isolation, and polling-place setup controls are operational/device concerns and are `N/A` or `DEFERRED` depending on deployment scope.

---

### Principle 7 — Marked, Verified, and Cast as Intended

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `7.1-*` Display, contrast, font, audio and control defaults
- `7.2-*` Navigation, interaction, operability, response time, inactivity alerts
- `7.3-*` Error feedback, review, overvote/undervote handling, instructions

**MiREMS mapping:**

- `VotingSessionPage` implements ballot presentation, selection controls, review, confirmation, receipt hash display.
- `ui-core` implements accessible buttons, inputs, modals, alerts, tables.
- Tests use axe-core, React Testing Library, and Playwright for critical flows.
- Overvote/undervote handling maps to ballot validation and review step.

**Notes:** Hardware controls, tactile keys, and physical dimensions are `N/A` unless MiREMS is deployed on certified voting terminals.

---

### Principle 8 — Robust, Safe, Usable, and Accessible

**Coverage status:** `PLANNED` / `N/A` mixed

**Relevant requirement groups:**

- `8.1-*` Hardware/accessory safety and assistive technology interfaces
- `8.2-A` Federal accessibility standards
- `8.3-A` Usability tests with voters
- `8.4-A` Usability tests with election workers

**MiREMS mapping:**

- Frontend accessibility target: WCAG 2.1 AA or stronger.
- Automated checks: axe-core in component and E2E tests.
- Kiosk UI: large targets, high contrast, keyboard navigation, ARIA roles.
- Usability evidence: `DEFERRED` to P9/P10 because it requires representative user testing.

**Notes:** Most `8.1-*` physical hardware/accessory requirements are `N/A` for the web/admin platform.

---

### Principle 9 — Auditable

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `9.1.1-A` Software independence
- `9.1.2-*` Tamper-evident records
- `9.1.3-*` Voter verification and error correction
- `9.1.4-*` Auditor verification
- `9.1.5-*` Paper records
- `9.1.6-*` E2E cryptographic verification
- `9.2-A` Audit support documentation
- `9.3-A` Data protection for audit records
- `9.4-*` Risk-limiting audit support

**MiREMS mapping:**

- Append-only audit: `AuditEvent`, `AuditEventPublisher`, Kafka topic `mirems.audit.events`.
- Vote immutability: `ADR-004`, immutable `VotingResult`, deterministic SHA-256 hash.
- Correction workflow: `VoteCorrectionProcess.bpmn`, dual approval, no in-place update.
- Audit review: `AuditReviewProcess.bpmn`, audit API restricted to `AUDITOR` and `SYSTEM_ADMIN`.
- RLA/statistical sampling: P8 advanced audit phase.

**Notes:** Paper record production and E2E cryptographic protocol verification are `DEFERRED` unless explicitly brought into MiREMS scope.

---

### Principle 10 — Ballot Secrecy

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `10.1-A` System use of voter information
- `10.2.1-*` Direct/indirect voter associations
- `10.2.2-*` Audit identifiers and ordering
- `10.2.3-*` Access to records of voter intent
- `10.2.4-*` Receipt/log/activation-device restrictions

**MiREMS mapping:**

- Encrypt/mask voter identifiers.
- Avoid exposing raw voter ID in DTOs, logs, receipts, and audit payloads.
- Receipts expose verification hash only, not ballot selections.
- Access to voting intent records restricted by RBAC and audited.
- Ballot secrecy data model review required before P1-012/P1-013 implementation.

**Notes:** The system must avoid linkability between voter identity and selections. This needs explicit schema and logging review before implementation.

---

### Principle 11 — Access Control

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `11.1-*` Logging activities/resource access and log integrity
- `11.2-*` Authorized access, role/group management, stage-based access, RBAC
- `11.3-*` Authentication, MFA, credential policy
- `11.4-*` Least privilege and separation of duties
- `11.5-*` Session controls and lockout

**MiREMS mapping:**

- Keycloak + OIDC/PKCE: `ADR-001`.
- Backend OAuth2 Resource Server: `mirems-auth`, `core-api` security.
- Role matrix: `docs/auth/ROLE_MATRIX.md`.
- Election-scoped authorization: `ElectionScopeValidator`, `@ElectionScoped`.
- Security audit events: `SECURITY_VIOLATION`, auth failures, role/scope violations.

**Notes:** MFA and credential policy enforcement are primarily Keycloak realm configuration and operational policy items.

---

### Principle 12 — Physical Security

**Coverage status:** `N/A` / `DEFERRED`

**Relevant requirement groups:**

- `12.1-*` Tamper evidence and physical access alerts
- `12.2-*` Secure containers, locks, ports, physical access points

**MiREMS mapping:**

- Hosted software components can log administrative access and deployment changes.
- Kubernetes/host hardening and infrastructure access logs are `DEFERRED` to P10.
- Physical voting device enclosure, seals, locks, and hardware ports are `N/A` unless MiREMS is deployed as part of a certified device package.

---

### Principle 13 — Data Protection

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `13.1-*` Configuration and election record protection
- `13.2-*` Cryptographic protection
- `13.3-*` Cryptographic module and key management
- `13.4-*` Network confidentiality/integrity

**MiREMS mapping:**

- AES-256 encryption for `VoterRecord.externalVoterId`.
- TLS/OIDC for API access.
- Hashes for `VotingResult` and `TabulationReport`.
- Environment-based secrets; no hardcoded keys.
- Future KMS/HSM/FIPS validation decision required for production.

**Notes:** FIPS cryptographic module validation and key ceremonies are `DEFERRED` until production hardening.

---

### Principle 14 — System Integrity

**Coverage status:** `PLANNED` / `DEFERRED`

**Relevant requirement groups:**

- `14.1-*` Risk assessment and attack surface reduction
- `14.2-*` Hardening and external network restrictions
- `14.3-*` Vulnerability management
- `14.4-*` Boot/software integrity and authorized updates

**MiREMS mapping:**

- CI/CD security gates: future GitHub Actions, dependency scanning, Trivy, OWASP ZAP.
- Container/Kubernetes hardening: P10.
- External network restrictions: deployment architecture and firewall policies.
- Dependency/version control: Gradle BOM, pnpm lockfile.

**Notes:** Secure boot and device update controls are `N/A` for pure web service deployment; `DEFERRED` for appliance/device packaging.

---

### Principle 15 — Detection and Monitoring

**Coverage status:** `PLANNED`

**Relevant requirement groups:**

- `15.1-*` Event logging
- `15.2-*` Error logging
- `15.3-*` Malware protection/detection/remediation
- `15.4-*` Network defense documentation and rule updates

**MiREMS mapping:**

- Structured logging: `core-infra` logging configuration.
- Audit event stream: Kafka `mirems.audit.events`.
- Actuator health/metrics: P0-008.
- Security events: auth failures, role violations, scope violations.
- Monitoring dashboards and runbooks: P10.

**Notes:** Host malware protection and network defense operations are deployment/operations controls and are `DEFERRED`.

---

## 4. Focused Traceability Matrix

| VVSG Area | Representative Requirement IDs | MiREMS Feature/Module | Status | Evidence / Future Goal |
|---|---|---|---|---|
| Election definition | `1.1.1-A`–`1.1.1-N` | `Election`, `Contest`, `Ballot`, `BallotStyle`, OpenAPI | `PLANNED` | P1-009 to P1-011, P3-029 |
| Pre-election readiness | `1.1.2-A`–`1.1.2-L` | Election publication BPMN, readiness checks | `PLANNED` | P2-021, P3-030 |
| Opening/closing voting mode | `1.1.3-*`, `1.1.7-*` | Election state machine, voting session lifecycle | `PLANNED` | P1-009, P1-017 |
| Vote capture and recording | `1.1.4-*`, `1.1.5-*` | `VotingSession`, `VotingResult`, kiosk UI | `PLANNED` | P1-012, P1-013, P5-052 |
| Ballot handling devices | `1.1.6-*` | Physical scanner/BMD integration | `N/A` | Out of current software-only scope |
| Tabulation | `1.1.8-*` | `BallotTabulationProcess`, `TabulationReport` | `PLANNED` | P2-024, P8 |
| Post-election reporting | `1.1.9-*` | Results API/dashboard, PDF/JSON export | `PLANNED` | P3-035, P5-053, P8 |
| Accuracy/reliability/stress | `1.2-*`, `2.7-*` | Unit/integration/E2E/load tests | `PLANNED` | P9 load/chaos testing |
| Software quality | `2.1-*`–`2.6-*` | Coding standards, modular architecture, tests | `PLANNED` | P0/P1 onward, CI P0-005 |
| Documentation transparency | `3.1-*`, `3.2-*`, `3.3-*` | README, ADRs, API docs, runbooks | `PLANNED` | P0-006, P3-038, P10 |
| Interoperability | `4.1-*`, `4.2-*` | OpenAPI, JSON export, future CDF mapping | `PLANNED` | P3-029, P8 |
| Device interfaces/COTS hardware | `4.3-*`, `4.4-*` | Physical hardware interfaces | `N/A` | Out of current software-only scope |
| Equivalent voter access | `5.1-*`, `5.2-*` | i18n, accessible UI, voting modes | `PLANNED` | P5-052, P5-056, P5-057 |
| Voter privacy | `6.1-*`, `6.2-A` | PII encryption, DTO masking, secrecy model | `PLANNED` | P1-012, P1-018, P5-052 |
| Marked/verified/cast as intended | `7.1-*`–`7.3-*` | Ballot UI, review step, overvote/undervote handling | `PLANNED` | P5-052 |
| Accessibility and usability | `8.2-A`, `8.3-A`, `8.4-A` | WCAG 2.1 AA, axe-core, user testing | `PLANNED` / `DEFERRED` | P5-052, P9/P10 |
| Hardware safety/accessories | `8.1-*` | Physical voting device hardware | `N/A` | Out of current software-only scope |
| Auditability | `9.1-*`–`9.4-*` | Immutable records, audit events, audit review, RLA | `PLANNED` | P1-013, P1-014, P2-027, P8 |
| Ballot secrecy | `10.1-A`, `10.2-*` | Data separation, restricted access, no selection logs | `PLANNED` | P1-012/P1-013 design review |
| Access control | `11.1-*`–`11.5-*` | Keycloak, RBAC, election scope, security logs | `PLANNED` | P0-004, P4-039 to P4-044 |
| Physical security | `12.1-*`, `12.2-*` | Deployment physical controls | `N/A` / `DEFERRED` | P10 operational docs if appliance scope appears |
| Data protection | `13.1-*`–`13.4-*` | Encryption, TLS, hashing, key management | `PLANNED` / `DEFERRED` | P1-012, P1-013, P10 |
| System integrity | `14.1-*`–`14.4-*` | CI security scanning, hardening, update control | `PLANNED` / `DEFERRED` | P0-005, P10 |
| Detection and monitoring | `15.1-*`–`15.4-*` | Structured logs, metrics, audit stream, dashboards | `PLANNED` | P0-008, P10 |

---

## 5. Volume II / Device Standards Applicability

MiREMS Platform is currently scoped as a software platform and web/admin/kiosk application, not as a physical voting machine, scanner, ballot marking device, printer, paper transport mechanism, lockable enclosure, or polling-place hardware package.

Therefore:

- Physical enclosure, seals, locks, ports, and tamper-evident container requirements are `N/A` for the current scope.
- Paper handling, scanner calibration, misfeed detection, ballot feeder separation, and mark-detection hardware requirements are `N/A` unless MiREMS later controls scanner/BMD devices.
- Electrical, EMC, environmental, transport, storage, and physical safety tests are `N/A` for hosted software deployments.
- If MiREMS is later distributed as an appliance or certified voting device package, these items must be reopened and mapped to hardware design, vendor documentation, VSTL test evidence, and chain-of-custody procedures.

---

## 6. Mandatory Implementation Rules Derived from This Mapping

1. Any feature touching `Election`, `Ballot`, `Candidate`, `VotingResult`, tabulation, certification, or audit must update this file if its VVSG posture changes.
2. `VotingResult` must remain immutable. Corrections must use `VoteCorrection` and BPMN dual approval.
3. Audit logs must be append-only and must avoid raw PII.
4. Frontend voting UI must satisfy WCAG 2.1 AA or stronger, verified by automated and E2E tests.
5. Core must not depend on country-specific extension code.
6. Authentication and authorization must be enforced in backend APIs even when the UI hides actions.
7. Receipts, logs, and audit payloads must never expose ballot selections in a way that links them to voter identity.
8. Physical/device requirements marked `N/A` must be reviewed if the deployment scope changes from software platform to voting device/appliance.

---

## 7. Open Compliance Questions

- Should MiREMS remain an election management and tabulation platform, or include certified vote capture/kiosk operation in production scope?
- Should Cast Vote Record export follow a specific NIST Common Data Format profile?
- Will production cryptography require FIPS 140-3 validated modules or HSM/KMS-backed key management?
- Will risk-limiting audit support be implemented in Core or as an extension pack?
- Will voter-facing kiosk flows be deployed on general browsers or controlled voting terminals?

These questions must be answered before claiming anything stronger than VVSG-aligned design.
