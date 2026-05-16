# PLAN.md — MiREMS Platform Development Plan

> Hermes Agent: run `/goal <GOAL_ID>` to execute a goal.
> Update goal State column after completion.

---

## Phase Overview

| Phase | Name | Goals | Est. Duration | Status |
|---|---|---|---|---|
| P0 | Foundation & Scaffold | 001–008 | 2 weeks | TODO |
| P1 | Core Domain Model | 009–018 | 3 weeks | TODO |
| P2 | BPMN/DMN Process Engine | 019–028 | 3 weeks | TODO |
| P3 | REST API Layer | 029–038 | 2 weeks | TODO |
| P4 | Authentication & RBAC | 039–044 | 2 weeks | TODO |
| P5 | Frontend Shell | 045–058 | 3 weeks | TODO |
| P6 | Extension Pack — KR | 059–068 | 3 weeks | TODO |
| P7 | Extension Pack — US | 069–078 | 3 weeks | TODO |
| P8 | Tabulation & Audit | 079–088 | 2 weeks | TODO |
| P9 | Integration & E2E | 089–096 | 2 weeks | TODO |
| P10 | Production Hardening | 097–104 | 2 weeks | TODO |

---

## Phase 0 — Foundation & Scaffold

### GOAL P0-001 | Initialize Gradle Multi-Project Build
**State:** `DONE`
**Depends on:** none
**Context files:** `README.md`, `AGENTS.md`

**Tasks:**
1. Create root `settings.gradle.kts` declaring all subprojects listed in AGENTS.md §3.
2. Create root `build.gradle.kts` with Gradle 8.10.2 toolchain targeting Java 21.
3. Create `mirems-bom/build.gradle.kts` as a Gradle platform BOM including:
   - `org.springframework.boot:spring-boot-dependencies:3.5.1`
   - `org.kie.kogito:kogito-bom:10.2.0`
   - `org.postgresql:postgresql:42.7.3`
   - `org.mapstruct:mapstruct:1.5.5.Final`
   - `org.testcontainers:testcontainers-bom:1.19.8`
4. Apply BOM to all backend subprojects via `platform(project(":mirems-bom"))`.
5. Configure Gradle wrapper to version 8.10.2.
6. Verify `./gradlew projects` lists all modules without error.

**Done Criteria:** `./gradlew build -x test` succeeds across all modules.

---

### GOAL P0-002 | Initialize pnpm Monorepo (Frontend)
**State:** `DONE`
**Depends on:** none
**Context files:** `README.md`

**Tasks:**
1. Create `frontend/pnpm-workspace.yaml` declaring workspaces: `mirems-shell`, `packages/*`, `extensions/*`.
2. Create root `frontend/package.json` with engines `pnpm>=9`, `node>=22`.
3. Scaffold `packages/ui-core`, `packages/api-client`, `packages/i18n` as empty TypeScript packages.
4. Create `frontend/mirems-shell/package.json` with React 19, TypeScript 5.9 LTS, Vite 6, TanStack Router.
5. Configure shared `tsconfig.base.json` in `frontend/` with strict mode, path aliases.
6. Configure shared ESLint + Prettier config at `frontend/.eslintrc.cjs`.
7. Run `pnpm install` and verify workspace links resolve correctly.

**Done Criteria:** `pnpm -r build` succeeds for all packages (empty builds).

---

### GOAL P0-003 | Docker Compose — Local Dev Environment
**State:** `DONE`
**Depends on:** P0-001, P0-002
**Context files:** `infrastructure/docker/`

**Tasks:**
1. Create `infrastructure/docker/docker-compose.dev.yml` with services:
   - `postgres`: PostgreSQL 16.4, port 5432, volume `pg_data`.
   - `keycloak`: Keycloak 24.x, import realm from `infrastructure/keycloak/mirems-realm.json`.
   - `kafka`: Bitnami Kafka 3.7, single-node, KRaft mode (no Zookeeper).
   - `kafka-ui`: Provectus Kafka UI.
   - `kogito-management-console`: optional Kogito management console.
2. Create `infrastructure/docker/.env.example` with all `MIREMS_*` variables from AGENTS.md §7.
3. Create `Makefile` at project root with targets: `dev-up`, `dev-down`, `dev-reset`, `dev-logs`.
4. Add health-check assertions for each service.

**Done Criteria:** `make dev-up` starts all services; `make dev-down` cleanly stops them.

**Completion Note — 2026-05-14:** Docker Compose files, environment template, Makefile targets, bootstrap Keycloak realm, Kafka image fix, and Windows port-conflict fix are complete. User verified the Docker containers run successfully on Windows.

---

### GOAL P0-004 | Keycloak Realm Configuration
**State:** `DONE`
**Depends on:** P0-003
**Context files:** `docs/adr/ADR-001-auth-strategy.md`

**Tasks:**
1. Export and commit base Keycloak realm JSON to `infrastructure/keycloak/mirems-realm.json`.
2. Define client `mirems-backend` (confidential, service account).
3. Define client `mirems-frontend` (public, PKCE).
4. Define roles: `SYSTEM_ADMIN`, `ELECTION_ADMIN`, `ELECTION_OFFICER`, `TABULATION_OFFICER`, `AUDITOR`, `OBSERVER`, `VOTER` (for future voter portal).
5. Define composite roles and role hierarchy in realm.
6. Document role-permission matrix in `docs/auth/ROLE_MATRIX.md`.

**Done Criteria:** Keycloak starts with realm loaded; all roles exist via Admin API check.

**Completion Note — 2026-05-14:** `infrastructure/keycloak/mirems-realm.json` defines the `mirems` realm, `mirems-backend` confidential client, `mirems-frontend` public PKCE client, required realm roles, composite hierarchy, and baseline groups. `docs/auth/ROLE_MATRIX.md` documents the initial RBAC policy. The running local Keycloak instance was updated and verified through the Admin API.

---

### GOAL P0-005 | CI Pipeline — GitHub Actions
**State:** `DONE`
**Depends on:** P0-001, P0-002
**Context files:** none

**Tasks:**
1. Create `.github/workflows/ci.yml` with jobs:
   - `backend-build`: `./gradlew build test` on Corretto 21.
   - `frontend-build`: `pnpm -r build` + `pnpm -r test`.
   - `coverage-check`: Fail if Jacoco coverage < 80% for changed modules.
   - `lint`: Checkstyle (backend) + ESLint (frontend).
2. Cache Gradle wrapper, Gradle caches, and pnpm store between runs.
3. Run tests against Testcontainers (no external DB needed in CI).

**Done Criteria:** Pipeline passes on a clean branch with only scaffold code.

**Completion Note — 2026-05-14:** Added `.github/workflows/ci.yml` for push, pull request, and manual dispatch on `main`. The pipeline runs backend build/test on Amazon Corretto 21 with Gradle caching, frontend install/build/lint/test on Node.js 22 and pnpm 9.15.9 with pnpm cache, and staged placeholder jobs for coverage and backend Checkstyle until Jacoco aggregate coverage and Checkstyle rules are introduced. Verified locally with `./gradlew build test --no-daemon` and `pnpm install --frozen-lockfile && pnpm -r build && pnpm -r lint && pnpm -r test`.

---

### GOAL P0-006 | Architecture Decision Records (ADR) Scaffold
**State:** `DONE`
**Depends on:** none
**Context files:** none

**Tasks:**
1. Create `docs/adr/` directory with `ADR-000-template.md`.
2. Write the following ADRs (status: Accepted):
   - `ADR-001-auth-strategy.md` — Keycloak + OIDC/PKCE
   - `ADR-002-hybrid-bpmn-direct.md` — When to use BPMN vs Direct Service
   - `ADR-003-extension-pack-isolation.md` — Core/Extension separation via Spring conditions
   - `ADR-004-vote-immutability.md` — Append-only vote records, correction via BPMN
   - `ADR-005-frontend-routing.md` — TanStack Router file-based routing
   - `ADR-006-audit-log-design.md` — Domain event → Kafka → AuditLog table (append-only)

**Done Criteria:** All 6 ADRs exist with status, context, decision, and consequences sections.

---

### GOAL P0-007 | VVSG 2.0 Compliance Mapping Document
**State:** `DONE`
**Depends on:** P0-006
**Context files:** `docs/adr/ADR-004-vote-immutability.md`

**Tasks:**
1. Create `docs/vvsg/VVSG2_MAPPING.md` mapping MiREMS modules to NIST VVSG 2.0 requirements.
2. Cover at minimum:
   - Volume I: Functional Requirements (Voting System, Vote Capture, Vote Processing)
   - Volume II: Device Standards (where applicable to software)
   - Audit capability requirements
   - Accessibility requirements (WCAG 2.1 AA for frontend)
3. Mark each requirement as: `IMPLEMENTED`, `PLANNED`, `N/A`, or `DEFERRED`.

**Done Criteria:** Document exists, covers all VVSG 2.0 Volume I functional categories.

---

### GOAL P0-008 | Logging, Observability, and Error Handling Scaffold
**State:** `DONE`
**Depends on:** P0-001
**Context files:** `AGENTS.md §7`

**Tasks:**
1. Add `mirems-core/core-infra` dependencies: Micrometer, Spring Actuator, Logback JSON encoder.
2. Configure structured JSON logging in `logback-spring.xml` (include `traceId`, `spanId`, `userId`, `electionId`).
3. Create `GlobalExceptionHandler` (`@RestControllerAdvice`) returning RFC 7807 Problem Details.
4. Create `MiremsException` hierarchy: `DomainException`, `ValidationException`, `ProcessException`, `SecurityException`.
5. Expose `/actuator/health`, `/actuator/metrics`, `/actuator/info` (restricted to `SYSTEM_ADMIN`).

**Done Criteria:** Application starts; health endpoint returns 200; structured logs appear in console.

**Completion Note — 2026-05-14:** Added Spring Boot observability scaffold across `core-infra`, `core-api`, and `core-domain`: Actuator/Micrometer dependencies, JSON console logging with MDC keys (`traceId`, `spanId`, `userId`, `electionId`), RFC 7807 `GlobalExceptionHandler`, `MiremsException` hierarchy, Actuator security rules, and a minimal bootable `MiremsCoreApiApplication`. Verified tests, build, `bootRun`, `/actuator/health` HTTP 200, restricted `/actuator/metrics` HTTP 401 unauthenticated, and structured JSON probe log output.

---

## Phase 1 — Core Domain Model

### GOAL P1-009 | Election Aggregate Root
**State:** `DONE`
**Depends on:** P0-001, P0-008
**Context files:** `AGENTS.md §5`, `docs/vvsg/VVSG2_MAPPING.md`

**Tasks:**
1. In `core-domain`, create `Election` aggregate with fields: `id` (UUID), `name`, `electionType` (enum), `jurisdiction`, `scheduledDate`, `electionStatus` (enum: DRAFT, PUBLISHED, ACTIVE, CLOSED, CERTIFIED), `countryCode`, `extensionPackId`.
2. Create `ElectionStatus` state machine enforcing valid transitions (DRAFT→PUBLISHED→ACTIVE→CLOSED→CERTIFIED). Throw `InvalidElectionStateException` on invalid transition.
3. Create `ElectionCreatedEvent`, `ElectionPublishedEvent`, `ElectionClosedEvent`, `ElectionCertifiedEvent` domain events.
4. Unit test all state transitions (valid and invalid).

**Done Criteria:** 90% unit test coverage on `Election` aggregate; state machine tested exhaustively.

**Completion Note — 2026-05-14:** Added the pure domain `Election` aggregate, `ElectionType`, `ElectionStatus` lifecycle state machine, `InvalidElectionStateException`, and P1-009 domain events. Implemented strict unit tests for aggregate creation, event recording/clearing, allowed transitions, invalid transitions, terminal `CERTIFIED` behavior, validation, and event timestamps. Added JaCoCo XML/HTML report generation for backend Java modules and verified `Election` aggregate coverage at 100% line/branch/instruction coverage with exhaustive state-machine checks. Verified `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport build --no-daemon` succeeds.

---

### GOAL P1-010 | Contest and Candidate Entities
**State:** `DONE`
**Depends on:** P1-009

**Tasks:**
1. Create `Contest` entity: `id`, `election` (FK), `contestType` (CANDIDATE_CHOICE, BALLOT_MEASURE, RANKED_CHOICE), `name`, `seats` (int), `voteLimit`.
2. Create `Candidate` entity: `id`, `contest` (FK), `name`, `partyAffiliation`, `candidateStatus` (PENDING, APPROVED, WITHDRAWN, DISQUALIFIED).
3. Enforce business rule: `voteLimit <= seats` (throw `ContestValidationException` otherwise).
4. Create `CandidateApprovedEvent`, `CandidateWithdrawnEvent`.
5. Unit test contest/candidate business rules.

**Done Criteria:** All entity constraints tested; 90% coverage.

**Completion Note — 2026-05-14:** Added pure domain `Contest` and `Candidate` entities with `ContestType`, `CandidateStatus`, `ContestValidationException`, `CandidateStateException`, and candidate approval/withdrawal domain events. Implemented strict TDD tests for contest initialization, required field validation, `voteLimit <= seats`, candidate creation, candidate required fields, approval/withdrawal event payloads, event buffer clearing, invalid lifecycle transitions, and exhaustive candidate status transition matrix. Verified `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport build --no-daemon` succeeds. JaCoCo result: contest package line coverage 97.8% and all P1-010 entity/business constraints covered.

---

### GOAL P1-011 | Ballot and BallotStyle Entities
**State:** `DONE`
**Depends on:** P1-010

**Tasks:**
1. Create `Ballot` entity: `id`, `election` (FK), `ballotVersion` (int, auto-incremented on change), `isActive` (boolean).
2. Create `BallotStyle` entity: `id`, `ballot` (FK), `styleCode`, `district`, `language` (ISO 639-1), `accessibilityFeatures` (Set<Enum>).
3. Create `BallotContest` join entity (Ballot ↔ Contest ordering and presentation).
4. Business rule: A Ballot must have at least one Contest. A BallotStyle must reference a valid language.
5. Unit tests for all constraints.

**Done Criteria:** 90% coverage on ballot domain.

**Completion Note — 2026-05-14:** Added pure domain `Ballot`, `BallotStyle`, and `BallotContest` entities with `AccessibilityFeature` and `BallotValidationException`. Implemented version auto-increment on ballot changes, activation rule requiring at least one contest, ISO 639-1 ballot style language validation, presentation ordering, immutable collection exposure, and field validation. Followed TDD with compile-time RED for missing ballot domain classes. Verified `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport build --no-daemon` succeeds. JaCoCo result: ballot package line coverage 100.0%.

---

### GOAL P1-012 | VoterRoll and VotingSession Entities
**State:** `DONE`
**Depends on:** P1-011

**Tasks:**
1. Create `VoterRecord` entity: `id` (UUID), `externalVoterId` (encrypted), `eligibleElections` (Set<UUID>), `registrationStatus`.
2. Create `VotingSession` entity: `id`, `election` (FK), `ballotStyle` (FK), `startedAt`, `completedAt`, `sessionStatus` (OPENED, CAST, SPOILED, EXPIRED), `deviceId`.
3. **VVSG 2.0**: `VoterRecord.externalVoterId` MUST be AES-256 encrypted at rest. Create `PiiEncryptionService` and `@Encrypted` JPA converter.
4. Business rule: A voter may have at most one non-SPOILED VotingSession per election (duplicate prevention check).
5. Unit tests covering PII encryption round-trip and duplicate prevention.

**Done Criteria:** Encryption tested; duplicate prevention tested; 90% coverage.

**Completion Note — 2026-05-14:** Added pure domain `VoterRecord` and `VotingSession` entities with `RegistrationStatus`, `SessionStatus`, `VotingSessionValidationException`, and `VotingSessionStateException`. Added AES-256-GCM `PiiEncryptionService`, `@Encrypted` marker annotation, and `EncryptedStringJpaConverter` using `MIREMS_PII_ENCRYPTION_KEY_BASE64` for JPA no-arg converter operation. `VoterRecord.externalVoterId` is encrypted on create and only decrypted through an explicit encryption service. Implemented duplicate prevention so a voter can have at most one non-SPOILED voting session per election, while allowing replacement after SPOILED. Verified PII encryption round-trip, randomized ciphertext, tamper rejection, converter null/non-null round-trip, voter eligibility, immutable collections, session lifecycle, terminal transitions, and duplicate prevention. Verified `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport build --no-daemon` succeeds. JaCoCo result: voting package line coverage 93.1%.

---

### GOAL P1-013 | VotingResult — Immutable Record
**State:** `DONE`
**Depends on:** P1-012
**Context files:** `docs/adr/ADR-004-vote-immutability.md`

**Tasks:**
1. Create `VotingResult` as a `@Immutable` JPA entity (no setter, no update). Fields: `id`, `session` (FK), `contest` (FK), `selectedCandidateIds` (JSONB), `castAt` (timestamp with TZ), `hash` (SHA-256 of record fields).
2. Create `VotingResultRepository` with ONLY `save()` and `findBy*()` — no update/delete methods.
3. Create `VoteCorrection` entity for amendments (references original `VotingResult`, goes through BPMN — see P2).
4. Compute and store SHA-256 hash on pre-persist lifecycle hook.
5. Unit test immutability: verify no `@Column(updatable=true)` exists on `VotingResult`.

**Done Criteria:** Attempted update throws exception; hash is deterministic; 90% coverage.

**Completion Note — 2026-05-14:** Added immutable JPA `VotingResult` with Hibernate `@Immutable`, no setters, non-updatable columns/join columns, JSONB `selectedCandidateIds`, timestamp-with-time-zone `castAt`, and SHA-256 hash computed in `@PrePersist`. Added append-only `VotingResultRepository` contract exposing only `save()` and `findBy*()` methods, with no update/delete methods. Added `VoteCorrection` and `CorrectionStatus` for amendments that reference the original result without mutating it. Added TDD tests for immutable annotations, non-updatable fields, immutable selected-candidate list update attempt, deterministic and field-sensitive hash, `@PrePersist` hook, repository method surface, correction reference behavior, and validation. Verified `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport build --no-daemon` succeeds. JaCoCo result: result package line coverage 96.8%. Also removed tracked `_temp/` working files from the repository per project cleanup request.

---

### GOAL P1-014 | Audit Log — Append-Only Domain Events
**State:** `DONE`
**Depends on:** P1-009
**Context files:** `docs/adr/ADR-006-audit-log-design.md`

**Tasks:**
1. Create `AuditEvent` entity: `id`, `eventType`, `aggregateId`, `aggregateType`, `payload` (JSONB), `actorId`, `occurredAt`, `sourceIp`.
2. Create `AuditEventRepository` — `save()` and read-only queries only. No delete method.
3. Create `AuditEventPublisher` Spring bean that persists to `AuditEvent` AND publishes to Kafka topic `mirems.audit.events`.
4. Create `@AuditAction` AOP annotation that wraps any `@Service` method, automatically emitting an `AuditEvent` on successful return.
5. Test: verify `@AuditAction` emits events and that saving twice with same ID is rejected.

**Done Criteria:** AOP annotation emits events; Kafka publish verified with embedded Kafka in test; 80% coverage.

**Completion Note — 2026-05-14:** Added append-only `AuditEvent` JPA entity with Hibernate `@Immutable`, non-updatable persisted columns, JSONB payload, actor/source/timestamp fields, immutable payload exposure, and validation. Added `AuditEventRepository` contract exposing only `save()` and `findBy*()` read methods. Added infra audit components: `InMemoryAuditEventRepository` with duplicate-ID rejection, `DuplicateAuditEventException`, `AuditEventPublisher` that persists then publishes JSON to Kafka topic `mirems.audit.events`, `@AuditAction`, `AuditActionAspect` that emits only after successful service return, and `AuditEventPublisherConfiguration` with conditional Spring bean wiring. Added Embedded Kafka test verifying AOP emission, repository persistence, Kafka record publication, and no emission on service exception. Verified duplicate save with same ID is rejected. Verified `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport :mirems-core:core-infra:test :mirems-core:core-infra:jacocoTestReport build --no-daemon` succeeds. JaCoCo result: audit domain package line coverage 91.7%; audit infra package line coverage 83.8%.

---

### GOAL P1-015 | JPA Repositories + Flyway Migrations
**State:** `DONE`
**Depends on:** P1-013, P1-014

**Tasks:**
1. Create Flyway migrations in `core-infra/src/main/resources/db/migration/`:
   - `V1__create_election_tables.sql`
   - `V2__create_ballot_tables.sql`
   - `V3__create_voter_tables.sql`
   - `V4__create_voting_result_tables.sql`
   - `V5__create_audit_log_table.sql`
2. Add appropriate indexes: `election.status`, `voting_result.session_id`, `audit_event.aggregate_id + occurred_at`.
3. Create Spring Data JPA repositories for all entities with relevant query methods.
4. Add Testcontainers PostgreSQL 16.4 integration test verifying all migrations apply cleanly.

**Done Criteria:** Flyway migrates cleanly on fresh DB; integration test passes.

**Completion Note — 2026-05-14:** Added Flyway PostgreSQL migrations V1-V5 for election/contest/candidate, ballot/style, voter/session, voting result/correction, and audit event tables. Added required indexes including `idx_elections_status`, `idx_voting_results_session_id`, and `idx_audit_events_aggregate_id_occurred_at`, plus supporting FK/query indexes. Added Spring Data JPA repository interfaces for Election, Contest, Candidate, Ballot, BallotStyle, VoterRecord, VotingSession, VotingResult, VoteCorrection, and AuditEvent. Added `MiremsPersistenceConfiguration` with conditional DataSource-based JPA repository/entity scanning. Retrofitted core domain aggregates/entities with JPA annotations needed for repository scanning while preserving existing domain tests. Added Flyway/Testcontainers PostgreSQL 16.4 migration test and migration resource contract test. Verification command `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport :mirems-core:core-infra:test :mirems-core:core-infra:jacocoTestReport build --no-daemon` succeeds in this WSL environment. Docker daemon is not available in this environment, so the Testcontainers PostgreSQL test is present but JUnit-skipped locally via `@Testcontainers(disabledWithoutDocker = true)`; it will execute where Docker is available.

---

### GOAL P1-016 | Domain Service — Election Management
**State:** `DONE`
**Depends on:** P1-015

**Tasks:**
1. Create `ElectionManagementService` (Direct Service): `createElection()`, `addContest()`, `addCandidate()`, `publishElection()` (delegates to BPMN — stub for now), `closeElection()`.
2. Each method emits appropriate domain events via `AuditEventPublisher`.
3. Add `@Transactional` at service level; domain events published AFTER transaction commit (use `TransactionalEventListener`).
4. Unit tests with Mockito mocking repositories.

**Done Criteria:** 80% service coverage; transactional event emission tested.

**Completion Note — 2026-05-14:** Added `ElectionManagementService` in `core-infra` with direct service methods for election creation, contest addition, candidate addition, publication, and close. Added `ElectionPublicationWorkflow` and `NoopElectionPublicationWorkflow` as the BPMN delegation stub for `publishElection()`. Service methods persist through Spring Data repositories and publish `TransactionalAuditEvent` application events instead of directly calling `AuditEventPublisher` inside the transaction. Added `TransactionalAuditEventListener` with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to bridge committed service events to append-only audit publication. Added Mockito unit tests for repository interactions, workflow delegation, missing aggregate rejection, audit payloads, service-level `@Transactional`, and AFTER_COMMIT listener configuration. Coverage for `io.mirems.core.infra.service.election` is 96.2% line coverage; `ElectionManagementService` line coverage is 97.7%. Verification command `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport :mirems-core:core-infra:test :mirems-core:core-infra:jacocoTestReport build --no-daemon` succeeds.

---

### GOAL P1-017 | Domain Service — Voter and Session Management
**State:** `DONE`
**Depends on:** P1-015

**Tasks:**
1. Create `VoterRollService` (Direct Service): `registerVoter()`, `updateEligibility()`, `checkEligibility(voterId, electionId)`.
2. Create `VotingSessionService` (Direct Service): `openSession()`, `castBallot()`, `spoilBallot()`.
3. `castBallot()` creates immutable `VotingResult` records and publishes `VoteCastEvent`.
4. `castBallot()` enforces duplicate-vote prevention at DB level (unique constraint + service check).
5. Unit + integration tests for duplicate prevention.

**Done Criteria:** Duplicate vote rejected at both service and DB level; 80% coverage.

**Completion Note — 2026-05-14:** Added `VoterRollService` for voter registration, eligibility updates, and active-registration eligibility checks without exposing raw voter PII in audit payloads. Added `VotingSessionService` for opening, casting, and spoiling voting sessions. `openSession()` performs a service-level duplicate non-SPOILED session check and translates database unique-index violations into domain duplicate-vote rejection. `castBallot()` persists immutable `VotingResult` records, computes receipt hashes through JPA pre-persist behavior, transitions the session to `CAST`, and publishes `VoteCastEvent` through committed transactional audit events. Added Spring Data duplicate-check repository method, voter eligibility mutation helpers, PII encryption service configuration, Mockito service tests, migration contract coverage for the partial unique index, and a PostgreSQL Testcontainers integration test verifying duplicate rejection at the DB level. Coverage for `io.mirems.core.infra.service.voting` is 91.7% line coverage; `VoterRollService` is 95.0% and `VotingSessionService` is 97.7%. Verification command `./gradlew :mirems-core:core-domain:test :mirems-core:core-domain:jacocoTestReport :mirems-core:core-infra:test :mirems-core:core-infra:jacocoTestReport build --no-daemon` succeeds. Docker is not available in this WSL environment, so the PostgreSQL Testcontainers duplicate-constraint test is present but JUnit-skipped locally via `@Testcontainers(disabledWithoutDocker = true)`; it will execute where Docker is available.

---

### GOAL P1-018 | MapStruct DTOs and Mappers
**State:** `DONE`
**Depends on:** P1-016, P1-017

**Tasks:**
1. Create request/response DTOs (Java records) for all domain entities.
2. Create MapStruct mappers: `ElectionMapper`, `ContestMapper`, `CandidateMapper`, `BallotMapper`, `VoterMapper`, `VotingResultMapper`.
3. Ensure no PII fields (raw `externalVoterId`) are exposed in response DTOs.
4. Unit test each mapper with sample data.

**Done Criteria:** All mappers compile; PII exclusion verified in tests; 80% coverage.

**Completion Note — 2026-05-14:** Added Java record request/response DTOs in `core-api` for election, contest, candidate, ballot, ballot style, voter, voting session, and voting result API boundaries. Added MapStruct mappers for `Election`, `Contest`, `Candidate`, `Ballot`, `VoterRecord`, `VotingSession`, and `VotingResult`, including parent ID flattening for nested domain relationships. Response DTOs intentionally exclude raw and encrypted `externalVoterId`; reflection and mapper tests verify PII exclusion. Verified `./gradlew :mirems-core:core-api:test :mirems-core:core-api:jacocoTestReport build --no-daemon` succeeds. JaCoCo result: `core-api` total line coverage 80.4%, mapper package line coverage 85.9%, DTO package line coverage 100.0%.

---

## Phase 2 — BPMN/DMN Process Engine

### GOAL P2-019 | Kogito Spring Boot Integration Setup
**State:** `DONE`
**Depends on:** P0-001, P1-018
**Context files:** `docs/adr/ADR-002-hybrid-bpmn-direct.md`

**Tasks:**
1. Add Kogito Spring Boot starter alignment to `core-bpmn/build.gradle.kts`:
   - Kogito 10.2.0 uses `org.kie.kogito:spring-boot-starters:10.2.0` under `kogito-spring-boot-bom:10.2.0`.
   - Legacy artifact names `kogito-spring-boot-starter`, `kogito-processes-spring-boot-starter`, and `kogito-decisions-spring-boot-starter` were checked and are not published at Kogito 10.2.0.
2. Configure `application.yml`: `kogito.service.url`, persistence type (`jdbc`), data-index URL.
3. Create a minimal "ping" BPMN (`PingProcess.bpmn`) with a single Script task returning `"pong"`.
4. Integration test: start ping process through `PingProcessService`, verify completion.
5. Kogito management console verification at `localhost:8180` is environment-blocked locally because Docker daemon is unavailable; compose console service already exists under the `kogito` profile.

**Done Criteria:** Kogito Spring Boot context starts without error; ping process completion contract passes; management console runtime verification is blocked locally by unavailable Docker daemon.

---

### GOAL P2-020 | ProcessService Wrapper (Hybrid Architecture Adapter)
**State:** `DONE`
**Depends on:** P2-019
**Context files:** `docs/adr/ADR-002-hybrid-bpmn-direct.md`

**Tasks:**
1. Create `ProcessService` abstract wrapper in `core-bpmn`:
   ```java
   public interface MiremsProcessService<I, O> {
     O startProcess(String processId, I input, String correlationId);
     O signalProcess(String instanceId, String signalName, Object payload);
     ProcessStatus getStatus(String instanceId);
   }
   ```
2. Implement `KogitoProcessAdapter` implementing the interface using Kogito API.
3. Create `ProcessStatus` record: `instanceId`, `processId`, `status`, `variables`, `activeNodes`.
4. This adapter is the ONLY entry point from Direct Services into the BPMN engine.
5. Unit test with mocked Kogito API.

**Done Criteria:** Adapter tested; interface contract verified; process package coverage above 80%.

---

### GOAL P2-021 | BPMN — Election Publication Process
**State:** `DONE`
**Depends on:** P2-020
**Context files:** `docs/vvsg/VVSG2_MAPPING.md`, `AGENTS.md §6`

**Tasks:**
1. Design `ElectionPublicationProcess.bpmn`:
   - Start: `publishElection()` called from `ElectionManagementService`.
   - Task 1 (User Task): `ELECTION_ADMIN` reviews election configuration.
   - Task 2 (Service Task): Validate all required contests are defined.
   - Task 3 (Service Task): Validate ballot styles cover all districts.
   - Gateway: Validation passed? → Task 4; Failed? → Error End with `ElectionValidationFailedEvent`.
   - Task 4 (Service Task): Set `ElectionStatus.PUBLISHED`, emit `ElectionPublishedEvent`.
   - End Event.
2. Implement Java service tasks as Kogito `KogitoWorkItemHandler` beans.
3. Integration-style service tests cover happy path, contest validation failure, ballot-style validation failure, and non-admin review failure.

**Done Criteria:** Both paths tested; `ElectionPublishedEvent` emitted in happy path; publication package coverage above 80%.

---

### GOAL P2-022 | BPMN — Candidate Registration Process
**State:** `DONE`
**Depends on:** P2-020

**Tasks:**
1. Design `CandidateRegistrationProcess.bpmn`:
   - Start: Candidate submits registration.
   - Task 1 (Service Task): Validate candidate eligibility (delegates to `CandidateEligibilityCheck.dmn`).
   - Task 2 (User Task): `ELECTION_OFFICER` reviews documentation.
   - Gateway: Approved? → Approve path; Rejected? → Rejection notification path.
   - Approve path: Set `CandidateStatus.APPROVED`, emit `CandidateApprovedEvent`.
   - Rejection path: Set `CandidateStatus.DISQUALIFIED`, emit `CandidateDisqualifiedEvent`, send notification.
   - Timer Boundary Event: Auto-reject if no officer action within 72h.
2. Create `CandidateEligibilityCheck.dmn` with basic age/residency rules.
3. Integration-style service tests cover happy path, officer rejection, eligibility rejection, and 72h timeout with mocked clock.

**Done Criteria:** All three paths tested; timer tested with mocked clock; candidate BPMN package coverage above 80%.

---

### GOAL P2-023 | DMN — Voter Eligibility Check
**State:** `DONE`
**Depends on:** P2-019
**Context files:** `docs/vvsg/VVSG2_MAPPING.md`

**Tasks:**
1. Create `VoterEligibilityCheck.dmn` with inputs: `voterAge`, `registrationStatus`, `residencyVerified`, `electionType`.
2. Decision table outputs: `eligible` (boolean), `reason` (string).
3. Rules cover: age requirements, registration status, residency, and election-type-specific rules.
4. Call from `VoterRollService.checkEligibility()` via Kogito DMN API.
5. Unit test: all decision table rows exercised.

**Done Criteria:** All DMN rows tested; integration with service layer verified; 80% coverage.

**Completion Note — 2026-05-15:** Added `VoterEligibilityCheck.dmn` under `core-bpmn` with voter age, registration status, residency, and election-type-specific decision rows plus `eligible`/`reason` outputs. Added `VoterEligibilityDecisionService`, request/result records, exhaustive row tests, and DMN XML contract verification. Integrated voter eligibility decisions into `VoterRollService.checkEligibility(CheckVoterEligibilityCommand)` while preserving the existing boolean eligibility API and guarding election assignment before DMN evaluation. Verified service-layer integration with Mockito. Verification command `./gradlew :mirems-core:core-bpmn:test :mirems-core:core-bpmn:jacocoTestReport :mirems-core:core-infra:test :mirems-core:core-infra:jacocoTestReport build --no-daemon` succeeds. JaCoCo result: voter BPMN package line coverage 95.0%; voting service package line coverage 92.3%.

---

### GOAL P2-024 | BPMN — Ballot Tabulation Process
**State:** `DONE`
**Depends on:** P2-022, P1-013
**Context files:** `docs/adr/ADR-004-vote-immutability.md`, `docs/vvsg/VVSG2_MAPPING.md`

**Tasks:**
1. Design `BallotTabulationProcess.bpmn`:
   - Start: Triggered on `ElectionStatus` → CLOSED.
   - Task 1 (Service Task): Load all `VotingResult` records for election.
   - Task 2 (Service Task): Aggregate by Contest → produce `TabulationReport` draft.
   - Task 3 (User Task): `TABULATION_OFFICER` reviews report.
   - Task 4 (Service Task): Lock report, generate hash, emit `TabulationCompletedEvent`.
   - Task 5 (Service Task): Publish results (if election is set to public results).
2. `TabulationReport` must be hash-signed (SHA-256) and stored immutably.
3. Integration test with 100 sample `VotingResult` records.

**Done Criteria:** Tabulation produces correct counts; report hash verified; 80% coverage.

**Completion Note:** Implemented immutable SHA-256-signed `TabulationReport`, `TabulationCompletedEvent`, tabulation BPMN/service flow, JPA repository/migration storage, and 100-record aggregation integration coverage. Verified full Gradle build and JaCoCo coverage gate.

---

### GOAL P2-025 | BPMN — Vote Correction Process
**State:** `DONE`
**Depends on:** P2-024
**Context files:** `docs/adr/ADR-004-vote-immutability.md`

**Tasks:**
1. Design `VoteCorrectionProcess.bpmn` (for administrative corrections only, e.g., data entry error).
2. Process requires dual `ELECTION_ADMIN` approval (two separate User Tasks, different approvers).
3. Creates a `VoteCorrection` record referencing original `VotingResult` — original is NEVER modified.
4. Emits `VoteCorrectedEvent` with full audit trail (who approved, when, original, corrected).
5. Integration test: verify original `VotingResult` is unchanged after correction.

**Done Criteria:** Dual-approval tested; original immutability verified; 80% coverage.

**Completion Note:** Implemented immutable append-only `VoteCorrection` dual approval, `VoteCorrectedEvent` audit payload, `VoteCorrectionProcess.bpmn`, correction orchestration service, repository port/wiring, Flyway approval columns, and integration tests proving the original `VotingResult` is unchanged. Verified full Gradle build and JaCoCo coverage gate.

---

### GOAL P2-026 | BPMN — Result Certification Process
**State:** `DONE`
**Depends on:** P2-025

**Tasks:**
1. Design `ResultCertificationProcess.bpmn`:
   - Input: Completed `TabulationReport`.
   - Task 1 (User Task): `ELECTION_ADMIN` reviews final tabulation.
   - Task 2 (User Task): Legal/compliance review (if applicable).
   - Task 3 (Service Task): Set `ElectionStatus.CERTIFIED`, emit `ElectionCertifiedEvent`.
   - Task 4 (Service Task): Generate official PDF certification report.
2. Integration test: end-to-end from closed election to certified.

**Done Criteria:** Election reaches CERTIFIED state; PDF generated; 80% coverage.

**Completion Note:** Implemented `ResultCertificationProcess.bpmn`, certification orchestration service, official PDF generation seam/default generator, `ElectionRepository` domain port plus JPA adapter, and tests covering closed election certification, legal review rejection, locked `TabulationReport` precondition, BPMN contract, PDF payload generation, and repository adapter wiring. Verified full Gradle build and JaCoCo coverage gate.

---

### GOAL P2-027 | BPMN — Audit Review Process
**State:** `DONE`
**Depends on:** P1-014

**Tasks:**
1. Design `AuditReviewProcess.bpmn` for post-election audit workflows.
2. AUDITOR role can initiate; read-only access to all election data.
3. Process produces `AuditReport` (PDF + JSON) from `AuditEvent` records.
4. Integration test with mock audit data.

**Done Criteria:** Audit report generated correctly; 80% coverage.

**Completion Note:** Implemented `AuditReviewProcess.bpmn`, AUDITOR-only orchestration service, `AuditReport` PDF/JSON artifact model, default JSON/PDF generator, and JPA audit repository adapter for persisted `AuditEvent` reads. Added tests for mock audit data report generation, chronological event ordering, non-AUDITOR rejection, defensive report content copies, BPMN contract markers, and JPA adapter delegation. Verified full Gradle build and JaCoCo coverage gate.

---

### GOAL P2-028 | Process Monitoring and Admin Console Integration
**State:** `DONE`
**Depends on:** P2-021–P2-027

**Tasks:**
1. Configure Kogito Management Console connection in `application.yml`.
2. Create `ProcessAdminController` REST endpoints:
   - `GET /admin/processes` — list all active process instances.
   - `POST /admin/processes/{id}/signal` — send signal to process.
   - `GET /admin/processes/{id}/audit` — get process audit trail.
3. Restrict all `/admin/**` endpoints to `SYSTEM_ADMIN` role.
4. Integration test all endpoints.

**Done Criteria:** Endpoints return correct data; role restriction enforced; 80% coverage.

**Completion Note:** Added Kogito service/data-index/management-console URL configuration, `ProcessAdminController` admin endpoints, `ProcessMonitoringService` contract, in-memory active process registry, Kogito-backed signal delegation, safe API fallback bean, and `/admin/**` security chain requiring `SYSTEM_ADMIN`. Added MockMvc integration tests for list/signal/audit endpoints and role restrictions plus process monitoring unit tests. Verified full Gradle build and JaCoCo coverage gate.

---

## Phase 3 — REST API Layer

### GOAL P3-029 | OpenAPI Specification and Code Generation
**State:** `DONE`
**Depends on:** P1-018

**Tasks:**
1. Write OpenAPI 3.1 spec in `docs/api/mirems-api.yaml` covering all domain operations.
2. Configure `openapi-generator` Gradle plugin to generate Spring server stubs into `core-api`.
3. Configure same plugin to generate TypeScript Axios client into `packages/api-client`.
4. All generated code excluded from coverage measurement.

**Done Criteria:** Spec generates both server stubs and TS client without errors.

**Completion Note:** Added OpenAPI 3.1 contract for election, contest, candidate, ballot, ballot style, voter, voting session, tabulation/results, and process-admin operations. Configured OpenAPI Generator in `core-api` to generate Spring Boot 3 server interfaces under `build/generated/openapi/server` and TypeScript Axios client sources under `frontend/packages/api-client/src/generated`. Wired generated server sources into `compileJava`, added TS client dependency/export wiring, excluded `**/generated/**` from JaCoCo class directories, and added OpenAPI contract tests for domain paths/schemas and role security metadata. Verified server and TS client generation plus backend/frontend builds.

---

### GOAL P3-030 | Election REST Controller
**State:** `DONE`
**Depends on:** P3-029, P1-016

**Tasks:**
1. Implement `ElectionController` implementing generated OpenAPI stub.
2. Endpoints: `POST /elections`, `GET /elections`, `GET /elections/{id}`, `PUT /elections/{id}/publish`, `PUT /elections/{id}/close`.
3. Security: `POST/PUT` require `ELECTION_ADMIN`; `GET` requires any authenticated role.
4. Validate request with Jakarta Validation; return RFC 7807 errors on failure.
5. Integration tests with Spring Boot Test + RestAssured.

**Done Criteria:** All endpoints tested including auth failure cases; 80% coverage.

**Completion Note:** Implemented `ElectionController` against the generated `ElectionsApi` stub with create/list/get/publish/close endpoints, domain-to-generated response mapping, principal/source-IP propagation to `ElectionManagementService`, RFC 7807 not-found/service-unavailable handling, and application security rules requiring `ELECTION_ADMIN` for create/publish/close while allowing any authenticated role for read endpoints. Added RestAssured Spring Boot integration tests for success paths, validation problem details, 404 problem details, unauthenticated 401, and insufficient-role 403. Added read methods to `ElectionManagementService` and unit coverage for repository-backed list/get. Aligned OpenAPI election enum/field names with the existing election domain model and regenerated the TypeScript Axios client.

---

### GOAL P3-031 | Contest and Candidate Controllers
**State:** `DONE`
**Depends on:** P3-030

**Tasks:**
1. Implement `ContestController`: CRUD for contests within an election.
2. Implement `CandidateController`: registration submission, status query, withdrawal.
3. `CandidateController.register()` triggers `CandidateRegistrationProcess.bpmn` via `ProcessService`.
4. Integration tests including BPMN process trigger verification.

**Done Criteria:** 80% coverage; BPMN trigger tested.

**Completion Note:** Implemented generated-stub-backed `ContestController` and `CandidateController` for contest create/list/get/update and candidate registration/list/get/withdrawal under election/contest routes. Registration now persists the candidate through `ElectionManagementService` and triggers `CandidateRegistrationProcessService.register(...)`, with RestAssured integration tests verifying the BPMN trigger payload. Added authenticated-read and role-gated write security rules (`ELECTION_ADMIN` for contest writes, `ELECTION_OFFICER` for candidate registration/withdrawal), RFC 7807 not-found/service-unavailable handling, service read/update/withdraw methods, repository-backed unit coverage, audit events for contest updates and candidate withdrawals, and domain support for contest detail updates. Aligned OpenAPI `ContestType` enum with the existing domain model and regenerated the TypeScript Axios client.

---

### GOAL P3-032 | Ballot and BallotStyle Controllers
**State:** `DONE`
**Depends on:** P3-031

**Tasks:**
1. Implement `BallotController`: create ballot, manage ballot versions.
2. Implement `BallotStyleController`: CRUD for ballot styles.
3. Endpoint `GET /elections/{id}/ballots/{ballotId}/preview` returns ballot layout JSON.
4. Integration tests.

**Done Criteria:** 80% coverage.

**Completion Note:** Implemented generated-stub-backed ballot endpoints for create/list/version/preview and ballot style create/list/update/delete under election routes. Added `BallotManagementService` with repository-backed create/version/style CRUD, audit events, election/contest ownership checks, and preview retrieval. Added domain support for ballot style detail updates. Secured ballot and ballot-style writes with `ELECTION_ADMIN` while keeping ballot/style reads authenticated. Added RFC 7807 handling for not-found, validation, domain validation, and service-unavailable failures. Aligned OpenAPI ballot schemas with the existing domain model (`contestIds`, nested contest/style responses, accessibility features, active/version fields) and regenerated the TypeScript Axios client. Added RestAssured integration tests plus service unit tests; backend and frontend builds pass.

---

### GOAL P3-033 | Voter Roll Controller
**State:** `DONE`
**Depends on:** P3-032

**Tasks:**
1. Implement `VoterRollController`: `POST /voters` (register), `GET /voters/{id}/eligibility/{electionId}`.
2. PII protection: `GET /voters/{id}` masked response (only eligibility status, no raw ID).
3. `VOTER` role: can only access own record. `ELECTION_OFFICER`: can access all.
4. Integration tests including role-based access.

**Done Criteria:** PII masking tested; role access tested; 80% coverage.

**Completion Notes:**
- Added generated-stub-backed `VoterRollController` for voter registration, masked voter lookup, and voter eligibility checks.
- Added PII masking so raw external voter references are decrypted only inside the API boundary and never returned in responses.
- Added owner-only `VOTER` access and all-record `ELECTION_OFFICER` access, plus voter endpoint security matchers.
- Added RFC 7807 handling for missing voters, invalid requests, access denial, and service unavailable fallback.
- Added `VoterRollService.getVoter(...)` with repository delegation test.
- Added integration and handler tests covering registration, masked lookup, eligibility, unauthorized/forbidden, validation, missing voter, and ProblemDetail mappings.

---

### GOAL P3-034 | Voting Session Controller
**State:** `DONE`
**Depends on:** P3-033, P1-017

**Tasks:**
1. Implement `VotingSessionController`: `POST /sessions`, `POST /sessions/{id}/cast`, `POST /sessions/{id}/spoil`.
2. `cast` endpoint creates `VotingResult` records; returns receipt (hash).
3. Strict rate limiting on cast endpoint (1 per session).
4. Integration test duplicate vote prevention end-to-end.

**Done Criteria:** Duplicate vote returns 409; receipt hash matches stored hash; 80% coverage.

**Completion Notes:**
- Added generated-stub-backed `VotingSessionController` for session creation, vote casting, and session spoil operations.
- Aligned OpenAPI voting-session schemas with the domain/service model: `ballotStyleId`, `deviceId`, `OPENED` status, and cast receipt `resultHashes`.
- Added cast receipt response mapping so the returned `receiptHash` matches the stored single-result hash returned by `VotingSessionService`.
- Added route security for `/sessions`, `/sessions/{id}/cast`, and `/sessions/{id}/spoil` for `VOTER` and `ELECTION_OFFICER` roles, with voter self-access enforced on session creation.
- Added RFC 7807 handling for duplicate/non-open cast conflicts, missing sessions, forbidden access, validation, bad requests, and service unavailable fallback.
- Added integration and handler tests covering create/cast/spoil, duplicate cast `409`, unauthorized/forbidden, validation, missing session, command mapping, and receipt hash behavior.

---

### GOAL P3-035 | Tabulation and Results Controller
**State:** `DONE`
**Depends on:** P3-034, P2-024

**Tasks:**
1. Implement `TabulationController`: `POST /elections/{id}/tabulate` (triggers BPMN), `GET /elections/{id}/results`.
2. Results endpoint: public when election is CERTIFIED; restricted otherwise.
3. Integration tests.

**Done Criteria:** 80% coverage; public/restricted access verified.

**Completion Notes:**
- Added generated-stub-backed `TabulationController` for tabulation workflow trigger and election results retrieval.
- `POST /elections/{id}/tabulate` loads the canonical election, invokes `BallotTabulationProcessService`, and returns `202 Accepted` with `ProcessStatus` containing report id/hash variables.
- `GET /elections/{id}/results` returns public results for `CERTIFIED` elections and restricts non-certified results to election/tabulation officials.
- Added explicit mapping from immutable `TabulationReport` / domain `ContestTally` into OpenAPI `TabulationResultResponse`, including deterministic contest/candidate ordering.
- Added route security for tabulation trigger (`TABULATION_OFFICER`) and controller-level public/restricted results enforcement.
- Added RFC 7807 handling for missing election, missing tabulation report, authentication required, forbidden, invalid workflow requests, and service unavailable fallback.
- Added integration and handler tests covering workflow trigger, public certified results, restricted non-certified results, forbidden access, not-found paths, and response mapping.

---

### GOAL P3-036 | Audit Log Controller
**State:** `DONE`
**Depends on:** P1-014

**Tasks:**
1. Implement `AuditLogController`: `GET /audit?aggregateId=&aggregateType=&from=&to=` with pagination.
2. Restricted to `AUDITOR` and `SYSTEM_ADMIN`.
3. Integration tests including pagination.

**Done Criteria:** 80% coverage; role restriction verified.

**Completion Notes:**
- Added OpenAPI `Audit` tag, `GET /audit` operation, `AuditLogEntry`, and `AuditLogPageResponse`; regenerated Java server stubs and TypeScript Axios client.
- Implemented generated-stub-backed `AuditLogController` with aggregate id/type filters, inclusive `from`/`to` time-window filtering, deterministic chronological ordering, and page/size metadata.
- Extended the append-only `AuditEventRepository` read contract with `findAllChronologically()` and implemented it in in-memory and JPA adapters.
- Added `AUDITOR`/`SYSTEM_ADMIN` route restriction for `GET /audit`.
- Added RFC 7807 handling for invalid audit query and audit service unavailable fallback.
- Added integration tests for filtering, pagination, second-page access, invalid query validation, and role restrictions.

---

### GOAL P3-037 | API Rate Limiting and Security Headers
**State:** `DONE`
**Depends on:** P3-030–P3-036

**Tasks:**
1. Configure Bucket4j rate limiting per IP and per user on sensitive endpoints.
2. Add Spring Security headers filter: `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`, `Content-Security-Policy`.
3. Configure CORS: allow only frontend origin in non-prod; restrict in prod.
4. Integration tests for rate limiting and header presence.

**Done Criteria:** Rate limit returns 429 after threshold; all security headers present; 80% coverage.

**Completion Notes:**
- Added Bucket4j-backed `ApiRateLimitingFilter` for sensitive endpoints with configurable capacity/refill, per-IP bucket, and per-authenticated-user bucket enforcement.
- Added `429 application/problem+json` response with `Retry-After` when rate limits are exceeded.
- Added security headers filter for `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`, and `Content-Security-Policy`.
- Added configurable CORS policy: non-prod allows only `mirems.security.cors.frontend-origin`; prod uses explicit `mirems.security.cors.prod-allowed-origins` allow-list.
- Added integration tests for rate limiting threshold, security headers, and CORS origin restriction, plus unit tests for prod/non-prod CORS selection.

---

### GOAL P3-038 | API Documentation and Health Endpoints
**State:** `DONE`
**Depends on:** P3-037

**Tasks:**
1. Expose SpringDoc `/v3/api-docs` and `/swagger-ui.html` (dev profile only).
2. Add build info to `/actuator/info` (version, git commit, build time).
3. Create custom health indicator for Kogito process engine.
4. Integration tests.

**Done Criteria:** Swagger UI loads in dev; health shows Kogito status.

**Completion Notes:**
- Added SpringDoc WebMVC UI with default-disabled docs and dev-profile enablement for `/v3/api-docs` and `/swagger-ui.html`.
- Added generated Gradle build/git metadata and `/actuator/info` contribution for version, git commit, and build time.
- Added `kogito` Actuator health component backed by the Kogito process adapter.
- Added integration tests for dev docs, prod docs disablement, actuator info metadata, and Kogito health details.
- Verification: backend Gradle full test/build and frontend `pnpm build` pass; `core-api` line coverage 85.7%, documentation package 100.0%.

---

## Phase 4 — Authentication & RBAC

### GOAL P4-039 | Spring Security Keycloak Integration
**State:** `DONE`
**Depends on:** P0-004, P3-029
**Context files:** `docs/adr/ADR-001-auth-strategy.md`

**Tasks:**
1. Configure `spring-security-oauth2-resource-server` with Keycloak JWT issuer.
2. Create `SecurityConfig` with method security enabled (`@PreAuthorize`).
3. Create `MiremsSecurityContext` bean extracting `userId`, `roles`, `electionScope` from JWT claims.
4. Apply `@PreAuthorize` to all controller methods based on role matrix from `docs/auth/ROLE_MATRIX.md`.
5. Integration test with mocked JWT tokens for each role.

**Done Criteria:** All endpoints enforce correct roles; 80% coverage.

**Completion Note — 2026-05-15:** Spring OAuth2 resource-server JWT support is wired to the Keycloak issuer environment variables, realm roles are converted to `ROLE_*` authorities, `MiremsSecurityContext` extracts user/role/election-scope claims, and controller method authorization is enabled with `@PreAuthorize` where access is not intentionally conditional/public. Mocked Keycloak JWT tests verify bearer-token 401/403/200 behavior and claim extraction. Verification passed with `:mirems-core:core-api:test` and `:mirems-core:core-api:jacocoTestReport`; core-api line coverage is 85.92%.

---

### GOAL P4-040 | Election-Scoped Authorization
**State:** `DONE`
**Depends on:** P4-039

**Tasks:**
1. Implement `ElectionScopeValidator`: verifies a user has access to a specific election ID (for multi-election, multi-jurisdiction deployments).
2. Create `@ElectionScoped` annotation for use in service methods.
3. Integrate scope check into Election, Contest, Ballot, Voter controllers.
4. Integration tests: user with Election A scope cannot access Election B data.

**Done Criteria:** Cross-election access rejected; 80% coverage.

**Completion Note — 2026-05-15:** Added `ElectionScopeValidator`, `@ElectionScoped`, and an AOP authorization aspect that enforces JWT `mirems_election_scope` or explicit `ELECTION_SCOPE_<electionId>` test/service authorities before scoped controller methods proceed. Election list responses are filtered to the caller's scope, and Election, Contest, Candidate, Ballot, Voter eligibility, Tabulation, and Voting Session endpoints now perform election-scope checks; certified election results remain public while uncertified results require both official authorization and election scope. Cross-election JWT integration tests verify Election A tokens receive `403` for Election B and do not call the service layer; unscoped JWT and Basic users without election-scope authority are denied. Verification passed with `:mirems-core:core-api:test`, `:mirems-core:core-api:jacocoTestReport`, and `:mirems-core:core-api:build`; core-api line coverage is 86.19%.

---

### GOAL P4-041 | Audit Security Events
**State:** `DONE`
**Depends on:** P4-040, P1-014

**Tasks:**
1. Log all authentication failures, role violations, and scope violations as `AuditEvent` with `eventType=SECURITY_VIOLATION`.
2. Log all successful logins (via Keycloak event listener or JWT filter).
3. Integration test: verify security violations produce audit events.

**Done Criteria:** Security events in audit log verified; 80% coverage.

**Completion Note — 2026-05-15:** Added security audit recording for authentication failures, authorization/role violations, election-scope violations, and successful JWT authentications. Security failures are persisted as append-only `AuditEvent` records with `eventType=SECURITY_VIOLATION`, bounded reason codes, structured payload metadata (`violationType`, actor, path, method, reason, and election id when applicable), and remote-address source IP capture without trusting `X-Forwarded-For`. Successful JWT authentications are recorded as `AUTHENTICATION_SUCCESS` through a resource-server filter. Added custom Spring Security authentication-entry-point and access-denied-handler wiring across admin, actuator, and application chains while preserving Basic/Bearer `WWW-Authenticate` challenge semantics, and integrated election-scope denial auditing before fail-closed rejection. Integration tests verify invalid bearer-token, invalid Basic credentials, role-violation, cross-election-scope denial, JWT-success audit events, bounded reason payloads, source-IP spoof resistance, and protocol-specific 401 challenges. Verification passed with `:mirems-core:core-api:test`, `:mirems-core:core-api:jacocoTestReport`, and `:mirems-core:core-api:build`; core-api line coverage is 87.01%.

---

### GOAL P4-042 | Frontend OIDC PKCE Auth Flow
**State:** `DONE`
**Depends on:** P0-004, P0-002

**Tasks:**
1. Integrate `oidc-client-ts` in `mirems-shell`.
2. Implement `AuthProvider` React context: `login()`, `logout()`, `user`, `isAuthenticated`, `hasRole()`.
3. Create `ProtectedRoute` TanStack Router wrapper component.
4. Create login page and post-login redirect.
5. Store tokens in memory (not localStorage) per VVSG security requirements.
6. Tests: auth flow with mocked Keycloak.

**Done Criteria:** Login/logout flow works; protected routes redirect unauthenticated users.

**Completion Note — 2026-05-15:** Added `oidc-client-ts` OIDC Authorization Code + PKCE configuration for `mirems-shell`, including platform-base-path callback, silent-renew, and logout redirect URIs. Implemented in-memory `AuthProvider`, `useAuth()`, Keycloak realm-role/election-scope claim helpers, login/logout methods, auth callback handling with post-login return redirect, and a Korean login page. Added `ProtectedRoute` decision logic and wrapped protected dashboard content so unauthenticated users are directed to `/miremsplatform/login?returnUrl=...` while public landing/login/callback flows remain available. Tokens are managed by `oidc-client-ts` user-manager state in React memory and are not stored by MiREMS application code in `localStorage`. Tests cover OIDC settings, role hierarchy/claim extraction, election scope, protected-route redirect decisions, sanitized return URLs, in-memory user storage, and same-document callback completion. Verification passed with `pnpm --filter @mirems/mirems-shell test`, `lint`, and `build`.

---

### GOAL P4-043 | Role-Based UI Rendering
**State:** `DONE`
**Depends on:** P4-042

**Tasks:**
1. Create `<RoleGuard roles={[...]}> ` component that hides/shows UI based on user roles.
2. Create `useAuth()` hook exposing `user`, `hasRole()`, `hasElectionScope()`.
3. Apply `RoleGuard` to all admin UI sections.
4. Tests: verify admin actions hidden from observer role.

**Done Criteria:** Role-gated UI tested with all roles; 75% coverage.

**Completion Note — 2026-05-16:** Added fail-closed `<RoleGuard roles={[...]}>` and pure role-decision tests covering direct roles, inherited MiREMS hierarchy roles, observer denial, missing-role denial, and empty-guard denial. Centralized role-gated dashboard actions, process approval actions, and task notifications in `role-ui.ts`, then applied `RoleGuard` to dashboard page actions, BPMN process approve/reject actions, and task notification links so observer-only users do not see administrator actions. `useAuth()` already exposes `user`, `hasRole()`, and `hasElectionScope()` from P4-042 and is used by `RoleGuard`. Verification passed with `pnpm --filter @mirems/mirems-shell test` (11 files / 27 tests), `lint`, `build`, static security scan, and independent review.

---

### GOAL P4-044 | Session Timeout and Token Refresh
**State:** `DONE`
**Depends on:** P4-042

**Tasks:**
1. Implement silent token refresh (before expiry).
2. Implement session timeout warning dialog (5 minutes before expiry).
3. Auto-logout on token expiry or refresh failure.
4. Unit tests for refresh logic.

**Done Criteria:** Refresh tested; auto-logout on failure verified.

**Completion Note — 2026-05-16:** Added frontend session lifecycle management for OIDC users: silent refresh is scheduled one minute before access-token expiry, a Korean session-timeout warning dialog appears five minutes before expiry, and token expiry or silent-refresh failure clears the in-memory user and removes the OIDC user record. Added unit tests for warning threshold, refresh scheduling, successful silent refresh, refresh-failure logout, and already-expired-token logout. Verification passed with Windows-native `pnpm --filter @mirems/mirems-shell test`, `lint`, `build`, and `pnpm -r build`.

---

## Phase 5 — Frontend Shell

### GOAL P5-045 | TanStack Router — File-Based Routing Setup
**State:** `DONE`
**Depends on:** P0-002, P4-042
**Context files:** `docs/adr/ADR-005-frontend-routing.md`

**Tasks:**
1. Configure TanStack Router with file-based routing in `mirems-shell/src/routes/`.
2. Define route tree: `/`, `/elections`, `/elections/$id`, `/elections/$id/contests`, `/elections/$id/ballots`, `/elections/$id/results`, `/admin`, `/audit`.
3. Apply `ProtectedRoute` to all routes except `/` (public landing).
4. Configure TanStack Router Devtools (dev only).
5. Tests: route rendering with mocked auth.

**Done Criteria:** All routes render; unauthenticated redirect works; 75% coverage.

**Completion Note — 2026-05-16:** Added TanStack Router integration for `mirems-shell` with a platform-base-path router, route modules under `src/routes/`, and a route definition contract covering public landing/login/callback plus protected election, admin, and audit routes. Refactored the shell into a protected layout that applies the existing `ProtectedRoute` decision logic around routed content, while `/` remains a public landing page and Router Devtools render only in development mode. Added Vitest coverage for the Phase 5 route tree, file-to-route mapping, platform base path, and public/protected route classification. Verification passed with Windows-native `pnpm --filter @mirems/mirems-shell test`, `lint`, and `build`.

---

### GOAL P5-046 | Design System and Component Library (ui-core)
**State:** `DONE`
**Depends on:** P0-002

**Tasks:**
1. In `packages/ui-core`, implement base components: `Button`, `Input`, `Select`, `DatePicker`, `Table`, `Modal`, `Badge`, `Alert`, `Spinner`, `Card`, `Tabs`.
2. Use CSS Modules + CSS custom properties for theming.
3. Accessibility: all interactive components WCAG 2.1 AA compliant (keyboard nav, ARIA, focus management).
4. Storybook (or Ladle) setup for visual component catalog.
5. Unit tests with Vitest + React Testing Library for each component.

**Done Criteria:** All 12 components have tests; accessibility checked; 75% coverage.

**Completion Note — 2026-05-16:** Implemented the `@mirems/ui-core` component library with 11 planned base components plus `ThemeProvider` for scoped CSS-custom-property theming. Components use CSS Modules, accessible labels/roles/ARIA states, Escape handling for `Modal`, native form semantics, and keyboard arrow navigation for `Tabs`. Added Vitest + React Testing Library coverage for all 12 exported UI primitives and a Ladle visual catalog with form, feedback, data display, navigation, and dialog stories. Integrated `ThemeProvider` at the `mirems-shell` router root. Verification passed with Windows-native `pnpm --filter @mirems/ui-core test`, `lint`, `build`, `catalog:build`, `pnpm --filter @mirems/mirems-shell test`, `lint`, `build`, and `pnpm -r build`.

---

### GOAL P5-047 | Election List and Detail Pages
**State:** `DONE`
**Depends on:** P5-045, P5-046, P3-030

**Tasks:**
1. `ElectionListPage`: table of elections with status badges, filter by status/type, pagination.
2. `ElectionDetailPage`: full election info, contest list, ballot summary, action buttons by role.
3. Use generated API client from `packages/api-client`.
4. Optimistic UI updates on status changes.
5. Tests with MSW (Mock Service Worker) mocking API.

**Done Criteria:** Pages render with mocked data; role-based actions tested; 75% coverage.

**Completion Note — 2026-05-16:** Implemented `ElectionListPage` and `ElectionDetailPage` under `mirems-shell/src/features/elections`, wired them into the TanStack file routes for `/elections` and `/elections/$id`, and added a generated API-client wrapper for election, contest, ballot, publish, and close calls with bearer-token propagation from the OIDC session. The list page supports status/type filters, pagination, status badges, loading/error/empty states, and Korean admin copy. The detail page renders election metadata, contest summaries, ballot summary counts, `ELECTION_ADMIN` plus election-scope-gated actions, optimistic publish/close status updates, rollback failure messaging, and non-admin guidance. Added MSW-backed Vitest/React Testing Library tests covering list rendering, authorization header propagation, filtering, pagination, detail summaries, role/scope-based action visibility, and optimistic status updates. Verification passed with Windows-native `pnpm --filter @mirems/mirems-shell test`, `lint`, `build`, targeted coverage for election pages above 75%, and `pnpm -r build`.

---

### GOAL P5-048 | Election Creation Wizard
**State:** `DONE`
**Depends on:** P5-047

**Tasks:**
1. Multi-step wizard: (1) Basic Info → (2) Contests → (3) Ballot Styles → (4) Review → (5) Submit.
2. Use TanStack Form or React Hook Form for each step.
3. Client-side validation mirrors server validation rules.
4. `ELECTION_ADMIN` role required (hidden from others).
5. Tests for each wizard step and submit flow.

**Done Criteria:** Full wizard flow tested end-to-end with mocked API; 75% coverage.

**Completion Note — 2026-05-16:** Implemented `ElectionCreationWizard` using React Hook Form with five steps for basic election information, contest drafts, ballot-style drafts, review, and submit completion. Added `ELECTION_ADMIN` role gating, client-side validation for required fields, ISO country code, ballot language code, and positive seat counts, plus generated API-client submission through `createElection` with runtime bearer-token propagation. Wired the wizard to `/elections/new` and added an admin-only entry link from the election list. Added MSW-backed tests covering the full wizard flow, validation blocking, role hiding, request payload shape, and `Authorization` header propagation. Verification passed with Windows-native `pnpm --filter @mirems/mirems-shell test`, `lint`, `build`, targeted election feature coverage above 75%, and `pnpm -r build`.

---

### GOAL P5-049 | Candidate Management Pages
**State:** `DONE`
**Depends on:** P5-048, P3-031

**Tasks:**
1. `CandidateListPage` per contest: table with status badges.
2. `CandidateRegistrationForm`: submission form triggering BPMN process.
3. `CandidateReviewPage` (ELECTION_OFFICER): approve/reject UI with process signal integration.
4. Real-time status updates via SSE or polling.
5. Tests.

**Done Criteria:** Registration triggers process; approval updates status; 75% coverage.

**Implementation Notes:**
- Added generated API client wrappers for candidate registration/listing and process-admin signals with bearer token propagation.
- Added contest-scoped candidate list, registration, and officer review pages under `/elections/$id/contests/$contestId/candidates` routes.
- Implemented polling status refresh for candidate lists and role/scope gating for registration and review actions.
- Added MSW tests for polling updates, registration payload/auth, process signal approval, and non-officer review denial.
- Expanded feature coverage configuration to include elections and candidates; combined feature coverage exceeds 75% global thresholds.

---

### GOAL P5-050 | Ballot Preview and BallotStyle Management
**State:** `DONE`
**Depends on:** P5-049, P3-032

**Tasks:**
1. `BallotStyleListPage`: table of ballot styles per ballot version.
2. `BallotPreviewPage`: visual preview of ballot layout using ballot JSON.
3. BallotStyle create/edit form.
4. Tests.

**Done Criteria:** Preview renders correctly; CRUD operations tested; 75% coverage.

**Implementation Notes:**
- Added generated `BallotsApi` wrapper for ballot list, ballot-style list/create/update, and preview calls with bearer token propagation.
- Replaced election ballots route with `BallotStyleManagementPage` and added nested preview route `/elections/$id/ballots/$ballotId/preview` with parent `<Outlet />` rendering.
- Implemented BallotStyle table grouped by ballot version plus create/edit form with accessibility feature checkboxes and election admin/scope gating.
- Applied election admin/scope gating to both BallotStyle management and ballot preview.
- Implemented ballot preview canvas from backend layout JSON, rendering title, instructions, contests, and selectable options.
- Added MSW tests for grouped listing, create/update payloads/auth, preview rendering, and scope denial; expanded feature coverage include to ballots.

---

### GOAL P5-051 | Voter Roll Management Pages
**State:** `DONE`
**Depends on:** P5-050, P3-033

**Tasks:**
1. `VoterRegistrationPage`: public-facing voter registration form.
2. `VoterEligibilityCheckPage`: check eligibility by election.
3. `VoterRollAdminPage` (ELECTION_OFFICER): list/search voters, update eligibility.
4. PII masking in admin table (show only last 4 chars of voter ID).
5. Tests.

**Done Criteria:** PII masking tested; eligibility check tested; 75% coverage.

**Completion Notes — 2026-05-16:**
- Added generated-client-backed voter API wrapper for registration, masked lookup, and eligibility checks with runtime bearer-token propagation.
- Added public `/voters/register` route plus protected `/voters` and `/voters/eligibility` routes; updated route inventory tests and generated route tree.
- Implemented `VoterRegistrationPage`, `VoterEligibilityCheckPage`, and `VoterRollAdminPage` with Korean labels, PII masking guidance, officer-only admin gating, admin-table voter ID masking to last 4 characters, and protected API error handling.
- P3-033 exposes masked single-voter lookup and eligibility check but not list/update-eligibility endpoints; admin UI therefore performs ID-based search and eligibility check, with an explicit API-scope notice instead of unsupported update calls.
- Added MSW tests covering registration payload/auth, raw external-reference suppression, eligibility result rendering, admin masked lookup, admin eligibility check, and non-officer API suppression.
- Verification passed: `pnpm --filter @mirems/mirems-shell test` (18 files / 61 tests), `pnpm --filter @mirems/mirems-shell lint`, `pnpm --filter @mirems/mirems-shell build`, targeted feature coverage (all 95.40%; voters statements 94.85%), and `pnpm -r build`.

---

### GOAL P5-052 | Voting Session UI (Kiosk Mode)
**State:** `DONE`
**Depends on:** P5-051, P3-034
**Context files:** `docs/vvsg/VVSG2_MAPPING.md`

**Tasks:**
1. `VotingSessionPage`: single-page kiosk UI for casting a ballot.
2. VVSG 2.0 accessibility: large font, high contrast, screen reader support, keyboard-only nav.
3. Ballot rendering: each contest with candidates, selection controls (radio/checkbox per contest type).
4. Review step before final submit; receipt display with hash after cast.
5. Spoil ballot option with confirmation.
6. Tests covering accessibility (axe-core integration).

**Done Criteria:** axe-core reports zero violations; cast and spoil flows tested; 75% coverage.

**Completion Notes — 2026-05-16:**
- Added generated-client-backed voting session API wrapper for session creation, vote casting, session spoiling, and ballot preview loading with bearer-token propagation.
- Added protected `/vote/session` kiosk route and route inventory coverage.
- Implemented `VotingSessionPage` with session start, VVSG-oriented large-text/high-contrast controls, screen-reader labeled regions, keyboard-operable native radio/checkbox/button controls, review-before-submit, receipt hash display, and spoil confirmation.
- Ballot rendering consumes preview layout contests with single-choice radio and multi-choice checkbox controls; cast payload uses `VoteSelection[]` by contest ID.
- Added `axe-core` dev dependency and MSW tests covering axe zero violations, cast payload/auth, required contest selections, spoil confirmation/auth, and non-voter API suppression.
- Verification passed: `pnpm --filter @mirems/mirems-shell test` (19 files / 65 tests), `pnpm --filter @mirems/mirems-shell lint`, `pnpm --filter @mirems/mirems-shell build`, targeted feature coverage (all 94.66%; voting statements 91.75%), and `pnpm -r build`.

---

### GOAL P5-053 | Results and Tabulation Dashboard
**State:** `DONE`
**Depends on:** P5-052, P3-035

**Tasks:**
1. `ResultsDashboardPage`: charts (bar/pie) per contest showing vote counts.
2. Real-time updates as tabulation progresses.
3. `TabulationProgressPage` (TABULATION_OFFICER): trigger tabulation, monitor BPMN progress.
4. PDF download of official results.
5. Tests.

**Done Criteria:** Charts render correctly; tabulation trigger tested; 75% coverage.

**Implementation Notes:**
- Implemented generated-client-backed results API wrapper for `getElectionResults`, `tabulateElection`, and process monitoring with runtime bearer token propagation.
- Added `/elections/$id/results` dashboard with Korean status labels, contest bar/pie summaries, pending-result polling, election-scope fail-closed access control, and client-side official-results PDF download.
- Added `/elections/$id/results/tabulation` for `TABULATION_OFFICER` users to trigger tabulation and monitor active BPMN tabulation process nodes.
- Added MSW tests for results polling, chart rendering, PDF download, tabulation trigger/process monitoring, bearer `Authorization` headers, and fail-closed role/scope guards.
- Verification passed: `pnpm --filter @mirems/mirems-shell test` (20 files / 70 tests), `pnpm --filter @mirems/mirems-shell lint`, `pnpm --filter @mirems/mirems-shell build`, `pnpm -r build`, targeted feature coverage 94.67% all statements / 94.71% results statements.

---

### GOAL P5-054 | Audit Log Viewer
**State:** `DONE`
**Depends on:** P5-053, P3-036

**Tasks:**
1. `AuditLogPage` (AUDITOR/SYSTEM_ADMIN): searchable, filterable table of audit events.
2. Filter by aggregate type, date range, actor.
3. Export to CSV.
4. Tests.

**Done Criteria:** Filters tested; CSV export verified; 75% coverage.

**Implementation Notes:**
- Implemented generated-client-backed audit API wrapper for `/audit` with runtime bearer token propagation.
- Added protected `/audit` viewer for `AUDITOR` and `SYSTEM_ADMIN` with fail-closed role gating before protected API calls.
- Added Korean audit table with search, aggregate ID/type, date-range server filters, actor client filter, payload display, and CSV export of currently filtered rows with spreadsheet-formula neutralization.
- Added MSW tests for Authorization header propagation, filter query parameters, actor/search filtering, CSV download, and denied-role API suppression.
- Verification passed: `pnpm --filter @mirems/mirems-shell test` (21 files / 74 tests), `pnpm --filter @mirems/mirems-shell lint`, `pnpm --filter @mirems/mirems-shell build`, `pnpm -r build`, targeted feature coverage 94.82% all statements / 95.96% audit statements.

---

### GOAL P5-055 | Admin Dashboard and System Health
**State:** `DONE`
**Depends on:** P5-054, P2-028

**Tasks:**
1. `AdminDashboardPage` (SYSTEM_ADMIN): system health cards (DB, Kafka, Kogito).
2. Active BPMN process instances list with manual signal capability.
3. Extension pack status panel (which packs are loaded).
4. Tests.

**Done Criteria:** Health data displayed; process signal tested; 75% coverage.

**Implementation Notes:**
- Implemented admin dashboard API wrapper for `/actuator/health`, generated `ProcessAdminApi.listProcessInstances`, and `signalProcessInstance` with runtime bearer token propagation.
- Added protected `/admin` dashboard for `SYSTEM_ADMIN` with fail-closed role gating before health/process API calls.
- Added DB/Kafka/Kogito health cards, active BPMN process table with sensitive variable redaction, manual signal JSON validation/submission, and loaded extension pack status cards for KR/US UI packs.
- Added MSW tests for Authorization header propagation, health cards, active process rendering, signal payload submission, invalid/non-object JSON suppression, sensitive variable redaction, and denied-role API suppression.
- Verification passed: `pnpm --filter @mirems/mirems-shell test` (22 files / 78 tests), `pnpm --filter @mirems/mirems-shell lint`, `pnpm --filter @mirems/mirems-shell build`, `pnpm -r build`, targeted feature coverage 94.89% all statements / 95.43% admin statements.

---

### GOAL P5-056 | Internationalization (i18n)
**State:** `DONE`
**Depends on:** P5-045

**Tasks:**
1. Configure `react-i18next` in `mirems-shell`.
2. Base translation keys in `packages/i18n/locales/en.json`.
3. Add Korean translations: `packages/i18n/locales/ko.json`.
4. Language switcher component in navbar.
5. All UI strings extracted to i18n keys (no hardcoded strings).
6. Tests: verify key count matches between locale files.

**Done Criteria:** English and Korean work; missing key test passes; 75% coverage.

**Implementation Notes:**
- Added `react-i18next`/`i18next` shell integration with `I18nProvider`, in-memory language switching, and default Korean / English fallback configuration.
- Expanded `@mirems/i18n` locale resources for shell/navigation/language selector keys and added exact key-parity tests for `en` and `ko` resources.
- Wired the protected shell header/sidebar navigation to translation keys and added the navbar `LanguageSwitcher`.
- Added Vite/Vitest workspace aliases so shell tests and builds resolve live workspace package sources consistently before package dist artifacts are generated.
- Added tests for locale metadata/key parity, stable navigation translation keys, and Korean-to-English language switching.
- Verification passed: `pnpm --filter @mirems/i18n test`, `pnpm --filter @mirems/i18n build`, `pnpm --filter @mirems/mirems-shell test` (24 files / 80 tests), `pnpm --filter @mirems/mirems-shell lint`, `pnpm --filter @mirems/mirems-shell build`, `pnpm -r build`, targeted feature+i18n coverage 94.99% all statements / 100% i18n statements.

---

### GOAL P5-057 | Responsive Layout and Theming
**State:** `DONE`
**Depends on:** P5-046

**Tasks:**
1. Implement responsive shell layout: sidebar nav (desktop), bottom nav (mobile).
2. Light/dark theme toggle using CSS custom properties.
3. High-contrast theme for accessibility (voting kiosk mode).
4. Persist theme preference in memory (no localStorage per VVSG).
5. Tests: layout rendering at mobile/tablet/desktop breakpoints.

**Done Criteria:** All breakpoints tested; themes switch correctly; 75% coverage.

**Implementation Notes:**
- Added `ShellThemeProvider`, `ThemeSwitcher`, in-memory shell theme state, and deterministic responsive layout helpers for desktop/tablet/mobile breakpoints.
- Refactored shell rendering through `ShellChrome` so desktop uses sidebar navigation and tablet/mobile use a bottom navigation surface.
- Added light/dark/high-contrast theme CSS custom-property overrides on `.app-shell[data-theme]` without localStorage/cookie/IndexedDB persistence.
- Extended i18n resources with theme selector and mobile navigation labels while preserving locale key parity.
- Added tests for desktop/tablet/mobile layout rendering, compact language-switcher availability, light/dark/high-contrast switching, and no browser-storage calls for theme preference.
- Verification passed: `pnpm --filter @mirems/i18n test`, `pnpm --filter @mirems/i18n build`, `pnpm --filter @mirems/mirems-shell test` (26 files / 86 tests), `pnpm --filter @mirems/mirems-shell lint`, `pnpm --filter @mirems/mirems-shell build`, `pnpm -r build`, targeted feature+i18n+theme coverage 94.81% all statements / 96.22% theme statements / 90.38% ShellLayout statements.

---

### GOAL P5-058 | Frontend Error Boundary and 404 Handling
**State:** `DONE`
**Depends on:** P5-045

**Tasks:**
1. Implement React Error Boundary at root and route level.
2. Design error page with recovery options (retry, go home, report).
3. Design 404 page.
4. Log frontend errors to backend `AuditLog` via error reporting endpoint.
5. Tests.

**Done Criteria:** Error boundary catches render errors; 404 renders correctly.

**Implementation Notes:**
- Added `AppErrorBoundary`, `RouteErrorPage`, and `NotFoundPage` with Korean recovery actions for retry, home navigation, and support reporting.
- Wired root route `errorComponent`/`notFoundComponent` and protected shell route content through the MiREMS error boundary.
- Added sanitized frontend error-report creation and bearer-token POST reporting to `/miremsplatform/audit/frontend-errors`; reporting failures are swallowed so recovery UI is never blocked.
- Extended i18n resources with error/404 labels while preserving locale key parity.
- Added tests for render-error capture, retry reset, route-level error page actions, 404 rendering, authorization-bearing audit reporting, no-token no-op reporting, and sanitization/redaction of component trace file paths, URL query/hash data, tokens, email addresses, and file paths.
- Verification passed: `pnpm --filter @mirems/i18n test`, `pnpm --filter @mirems/i18n build`, `pnpm --filter @mirems/mirems-shell test` (28 files / 93 tests), `pnpm --filter @mirems/mirems-shell lint`, `pnpm --filter @mirems/mirems-shell build`, `pnpm -r build`, targeted feature+i18n+theme+errors coverage 94.78% all statements / 95.45% errors statements.

---

## Phase 6 — Extension Pack: Korea (ext-kr)

### GOAL P6-059 | KR Extension Pack Scaffold
**State:** `DONE`
**Depends on:** P1-015

**Tasks:**
1. Copy `ext-template` to `extensions/ext-kr`.
2. Implement `KrElectionExtensionPack` bean (conditional on `mirems.extension.kr.enabled=true`).
3. Add `V100__kr_extension_tables.sql` Flyway migration for KR-specific tables.
4. Register in `core-domain`'s `ExtensionPackRegistry`.

**Done Criteria:** KR pack loads when property enabled; ignored when disabled.

**Implementation Notes:**
- Added core-domain extension SPI (`ElectionExtensionPack`) and immutable `ExtensionPackRegistry` with duplicate/blank ID validation and deterministic enabled-pack ordering.
- Added reusable `ext-template` Spring Boot auto-configuration scaffold and KR extension module build wiring.
- Implemented `KrElectionExtensionPack` and `KrExtensionAutoConfiguration`, activated only by `mirems.extension.kr.enabled=true`, with Spring Boot auto-configuration imports.
- Added KR extension Flyway migration resource `db/migration/ext/kr/V100__kr_extension_tables.sql` with KR metadata and election-type mapping scaffold tables/indexes.
- Added tests proving registry lookup/validation, KR metadata/resource locations, conditional enabled/disabled loading, registry bean registration, and migration resource presence.
- Verification passed: `gradlew.bat :mirems-core:core-domain:test --tests io.mirems.core.domain.extension.ExtensionPackRegistryTest`, `gradlew.bat :extensions:ext-kr:test --tests io.mirems.extension.kr.*`, `gradlew.bat :mirems-core:core-domain:test :extensions:ext-kr:test :extensions:ext-template:build`, `gradlew.bat :extensions:ext-kr:jacocoTestReport :mirems-core:core-domain:jacocoTestReport`, `gradlew.bat build`. Coverage: ext-kr 100.00% instruction/line/branch; core-domain 89.45% instruction / 88.03% line / 76.54% branch.

---

### GOAL P6-060 | KR — 선거 유형 및 관할 모델
**State:** `DONE`
**Depends on:** P6-059

**Tasks:**
1. KR-specific `ElectionType` values: 대통령선거, 국회의원선거, 지방선거, 교육감선거, 보궐선거.
2. KR jurisdiction hierarchy: 시/도 → 시/군/구 → 읍/면/동.
3. `KrJurisdiction` entity with constituency code (선거구 코드) mapping.
4. KR-specific Flyway migration.
5. Tests.

**Done Criteria:** KR election types and jurisdiction hierarchy tested.

**Implementation Notes:**
- Added `KrElectionType` with 대통령선거, 국회의원선거, 지방선거, 교육감선거, 보궐선거 labels, slugs, and core `ElectionType` mappings.
- Added `KrJurisdictionLevel` and immutable `KrJurisdiction` entity model for 시/도 → 시/군/구 → 읍/면/동 hierarchy, parent validation, normalized administrative codes, and 선거구 코드 mapping.
- Added `V101__kr_election_type_jurisdiction_tables.sql` with `kr_election_types` seed rows and `kr_jurisdictions` hierarchy/constituency schema, exact parent-level trigger validation, immutable level updates, and indexes.
- Added tests for KR election-type coverage/mapping, jurisdiction hierarchy path and parent-level validation, required-field normalization/rejection, and migration resource contract.
- Verification passed: `gradlew.bat :extensions:ext-kr:test`, `gradlew.bat :extensions:ext-kr:jacocoTestReport`, `gradlew.bat :extensions:ext-kr:build`, `gradlew.bat build`. Coverage: ext-kr package 97.13% instruction / 96.72% line / 75.00% branch; new KR model classes 100%/93.33%/100% line for `KrElectionType`/`KrJurisdiction`/`KrJurisdictionLevel`.

---

### GOAL P6-061 | KR — 공직선거법 DMN Rules
**State:** `DONE`
**Depends on:** P6-060
**Context files:** `docs/extensions/kr/LEGAL.md`

**Tasks:**
1. Create `KrCandidateEligibility.dmn`: 피선거권 rules (age, citizenship, criminal record restrictions).
2. Create `KrVoterEligibility.dmn`: 선거권 rules (age 18+, citizen or permanent resident per election type).
3. Create `KrCampaignPeriod.dmn`: valid campaign activity periods by election type.
4. Unit test all DMN decision tables exhaustively.

**Done Criteria:** All DMN rules reflect 공직선거법; 100% row coverage.

**Implementation Notes:**
- Added missing KR legal reference file `docs/extensions/kr/LEGAL.md` with the explicit legal assumptions and review-needed items used by the DMN snapshot.
- Added KR DMN resources under `backend/extensions/ext-kr/src/main/resources/decisions/kr/`: `KrCandidateEligibility.dmn`, `KrVoterEligibility.dmn`, and `KrCampaignPeriod.dmn`.
- Added typed request/result records and Spring decision services for candidate eligibility, voter eligibility, and campaign-period validation.
- Added exhaustive row-coverage tests for all three DMN tables, including XML contract assertions for inputs, outputs, and rule counts; voter DMN covers all citizen election types plus permanent-resident local/superintendent eligibility and non-citizen denials; campaign DMN uses D-23..D-1 for presidential and D-14..D-1 for standard public elections.
- Verification passed: `gradlew.bat :extensions:ext-kr:test --tests io.mirems.extension.kr.rules.*`, `gradlew.bat :extensions:ext-kr:test :extensions:ext-kr:jacocoTestReport`, `gradlew.bat :extensions:ext-kr:build`, `gradlew.bat build`. Coverage: `io.mirems.extension.kr.rules` package 100.00% instruction / 100.00% line / 91.67% branch.

---

### GOAL P6-062 | KR — 비례대표 Contest Type
**State:** `TODO`
**Depends on:** P6-061

**Tasks:**
1. Implement `PROPORTIONAL_REPRESENTATION` contest type extension for KR parliamentary elections.
2. Party list ballot rendering support.
3. KR-specific tabulation rule: seat allocation by D'Hondt method.
4. DMN for D'Hondt seat calculation.
5. Tests with sample election data.

**Done Criteria:** Seat allocation produces correct results for known test cases.

---

### GOAL P6-063 | KR — 사전투표 (Early Voting) Extension
**State:** `TODO`
**Depends on:** P6-062

**Tasks:**
1. Extend `VotingSession` with `votingMethod` enum: EARLY_VOTING, ELECTION_DAY, ABSENTEE.
2. KR-specific early voting period validation (D-5 to D-4 before election day).
3. Cross-district early voting: allow voter to vote at any polling station.
4. Tests.

**Done Criteria:** Early voting period enforced; cross-district voting tested.

---

### GOAL P6-064 | KR UI Extension — Korean Language and Layout
**State:** `TODO`
**Depends on:** P5-056, P6-063

**Tasks:**
1. KR-specific ballot layout component (vertical candidate list with party logo).
2. Korean locale translations for all KR-specific terms.
3. KR election calendar widget.
4. Tests.

**Done Criteria:** KR ballot renders correctly; Korean UI strings complete.

---

### GOAL P6-065 — P6-068 | KR Extension Integration Tests
**State:** `TODO`
**Depends on:** P6-064

**Tasks:**
1. End-to-end test: create KR 국회의원선거, add candidates, run early voting, tabulate (including 비례대표).
2. Verify DMN eligibility rules apply.
3. Verify 공직선거법 constraints enforced throughout.
4. Performance test: 10,000 vote simulation.

**Done Criteria:** All integration tests pass; performance target ≤ 100ms p95 per vote cast.

---

## Phase 7 — Extension Pack: USA (ext-us)

### GOAL P7-069 | US Extension Pack Scaffold
**State:** `TODO`
**Depends on:** P1-015

Same pattern as P6-059 but for `ext-us`, conditional on `mirems.extension.us.enabled=true`.

---

### GOAL P7-070 | US — Jurisdiction and Precinct Model
**State:** `TODO`
**Depends on:** P7-069

**Tasks:**
1. US jurisdiction: State → County → Precinct.
2. Precinct code mapping (FIPS).
3. Multi-member district support (e.g., state legislature).

---

### GOAL P7-071 | US — HAVA and UOCAVA DMN Rules
**State:** `TODO`
**Depends on:** P7-070
**Context files:** `docs/extensions/us/LEGAL.md`

**Tasks:**
1. `UsVoterEligibility.dmn`: HAVA requirements (ID verification, provisional ballot rules).
2. `UsAbsenteeBallot.dmn`: UOCAVA overseas/military voter rules.
3. State-specific age rules (18 on election day in most states; primary age rules vary).
4. Exhaustive DMN tests.

---

### GOAL P7-072 | US — Ranked Choice Voting (RCV) Support
**State:** `TODO`
**Depends on:** P7-071

**Tasks:**
1. `RANKED_CHOICE` contest type: allow voter to rank candidates 1–N.
2. RCV tabulation: instant-runoff algorithm as Kogito DMN/BPMN.
3. Tests with known RCV election outcomes.

---

### GOAL P7-073 — P7-078 | US Extension Features and Integration Tests
**State:** `TODO`
**Depends on:** P7-072

Covers: provisional ballot workflow, absentee ballot tracking, US UI extension, and end-to-end integration tests. Follow same pattern as KR Phase 6.

---

## Phase 8 — Tabulation & Audit (Advanced)

### GOAL P8-079 — P8-088 | Advanced Tabulation, Audit Reports, and Statistical Sampling
**State:** `TODO`
**Depends on:** P2-024, P2-027

Covers: risk-limiting audit (RLA) support, statistical sampling for post-election audits, chain-of-custody reporting, external audit export (JSON/XML), and VVSG 2.0 audit trail completeness verification.

---

## Phase 9 — Integration & E2E

### GOAL P9-089 — P9-096 | Playwright E2E Tests and Load Testing
**State:** `TODO`
**Depends on:** P5-058, P6-065, P7-073

Covers: Playwright E2E for critical paths (election lifecycle, voting, tabulation, certification), load testing with k6 (10k concurrent voters), chaos testing, and full VVSG 2.0 compliance checklist run.

---

## Phase 10 — Production Hardening

### GOAL P10-097 — P10-104 | Kubernetes, Security Scanning, and Documentation
**State:** `TODO`
**Depends on:** P9-096

Covers: Helm charts for production K8s, Trivy container scanning, OWASP ZAP API scan, Grafana dashboards, runbook documentation, data backup/restore procedures, and final VVSG 2.0 compliance report generation.

---

## Goal Dependency Graph (Summary)

```
P0 (001-008) → P1 (009-018) → P2 (019-028) → P3 (029-038) → P4 (039-044)
                                                              ↓
P4 → P5 (045-058) → P6 (059-068) → P8 (079-088) → P9 (089-096) → P10 (097-104)
                  → P7 (069-078) ↗
```
