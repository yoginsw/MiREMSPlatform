# AGENTS.md — MiREMS Platform AI Agent Instructions

> **For Hermes Agent** | Use `/goal <GOAL_ID>` to execute a specific goal.
> All agents MUST read this file before executing any goal.

---

## 1. Project Identity

| Item | Value |
|---|---|
| Project | MiREMS (Miru Election Management Solution) Platform |
| Version | 0.1.0-SNAPSHOT |
| Standard | NIST VVSG 2.0 compliant |
| Architecture | Core + Extension Pack Hybrid (BPMN/DMN + Direct Service) |

---

## 2. Agent Ground Rules

### 2.1 Non-Negotiable Constraints

1. **VVSG 2.0 Compliance First** — Every feature touching vote data, ballot, or audit MUST align with NIST VVSG 2.0 requirements. Reference `docs/vvsg/VVSG2_MAPPING.md` before implementing.
2. **No Direct DB Access from UI** — All data flows through the REST/GraphQL API layer. Never write frontend code that calls DB directly.
3. **Audit Log Everything** — Any state change to `Election`, `Ballot`, `Candidate`, `VotingResult` entities MUST emit an audit event via `AuditEventPublisher`.
4. **Immutable Vote Records** — Once a `VotingResult` is committed, no UPDATE is allowed. Corrections go through a separate `VoteCorrection` BPMN process.
5. **Extension Pack Isolation** — Core code MUST NOT import any class from `ext-*` packages. Extensions depend on Core, not the reverse.
6. **Test Coverage Gate** — Do not mark a goal DONE unless unit test coverage ≥ 80% for the changed module and all existing tests pass.
7. **Token Budget Discipline** — Read only the files you need. Do not load entire modules into context when a single class suffices.

### 2.2 Coding Standards

- Java: Google Java Style Guide. Max line length 120. Prefer records for DTOs.
- TypeScript: ESLint + Prettier (config in `frontend/.eslintrc.cjs`). Strict mode on.
- BPMN/DMN: Files live in `src/main/resources/processes/`. One BPMN per business process.
- SQL: Flyway migrations in `src/main/resources/db/migration/`. Format: `V{version}__{description}.sql`.
- Commit message: `[GOAL-{id}] {type}: {short description}` (type = feat|fix|refactor|test|docs).

### 2.3 Hybrid Architecture Decision Rule

Before implementing a new service, apply this decision tree:

```
Is the operation a long-running, multi-party, auditable workflow?
  YES → Implement as Kogito BPMN process (use ProcessService wrapper)
  NO  → Is it a stateless business rule / policy decision?
          YES → Implement as Kogito DMN (Decision Model)
          NO  → Implement as Direct Spring Service (@Service)
```

Examples:
- `ElectionPublicationProcess.bpmn` → BPMN (multi-step approval)
- `EligibilityCheck.dmn` → DMN (rule engine)
- `DistrictCrudService.java` → Direct Service (simple CRUD)

---

## 3. Repository Layout (Quick Reference)

```
mirems-platform/
├── backend/
│   ├── mirems-bom/              # Dependency BOM (Gradle platform)
│   ├── mirems-core/             # Core domain + infrastructure
│   │   ├── core-domain/         # Entities, Aggregates, Domain Events
│   │   ├── core-bpmn/           # Kogito BPMN/DMN process definitions
│   │   ├── core-api/            # REST controllers, OpenAPI spec
│   │   └── core-infra/          # JPA repos, Flyway, Kafka adapters
│   ├── mirems-auth/             # Keycloak integration, RBAC
│   └── extensions/
│       ├── ext-common/          # Shared extension utilities
│       ├── ext-us/              # US election pack (HAVA, UOCAVA)
│       ├── ext-kr/              # Korean election pack (공직선거법)
│       └── ext-template/        # Scaffold for new country packs
├── frontend/
│   ├── mirems-shell/            # Main React 19 shell app
│   ├── packages/
│   │   ├── ui-core/             # Shared component library
│   │   ├── api-client/          # Generated OpenAPI client
│   │   └── i18n/                # Translation keys
│   └── extensions/
│       ├── ext-us-ui/           # US-specific UI modules
│       └── ext-kr-ui/           # KR-specific UI modules
├── infrastructure/
│   ├── docker/                  # Compose files per environment
│   └── k8s/                     # Helm charts
├── docs/
│   ├── vvsg/                    # VVSG 2.0 mapping documents
│   ├── adr/                     # Architecture Decision Records
│   └── api/                     # OpenAPI specs
├── AGENTS.md                    # ← This file
├── PLAN.md                      # Phase/Goal definitions
└── README.md                    # Project overview
```

---

## 4. Goal Execution Protocol

### 4.1 How to Run a Goal

```bash
/goal PHASE1-001
```

Hermes will:
1. Load `PLAN.md` and locate the goal definition.
2. Read `AGENTS.md` ground rules.
3. Read any `CONTEXT` files listed in the goal.
4. Execute tasks in order, verifying each step.
5. Run tests and confirm the coverage gate.
6. Report DONE or BLOCKED with reason.

### 4.2 Goal States

| State | Meaning |
|---|---|
| `TODO` | Not started |
| `IN_PROGRESS` | Agent is actively working |
| `DONE` | Completed, tests pass, coverage ≥ 80% |
| `BLOCKED` | Requires human decision or upstream goal |
| `SKIPPED` | Deferred by human decision |

### 4.3 When Blocked

If a goal cannot be completed (missing spec, upstream dependency, ambiguous requirement), the agent MUST:
1. Write a `BLOCKED` note in `PLAN.md` under the goal.
2. List the exact blocking reason and what human input is needed.
3. Stop and do NOT proceed to downstream goals.

---

## 5. Key Domain Vocabulary

Agents must use these exact terms in code, comments, and commit messages.

| Term | Meaning |
|---|---|
| `Election` | A single election event (e.g., "2024 Presidential Election - US") |
| `ElectionType` | Enum: PRESIDENTIAL, PARLIAMENTARY, REGIONAL, REFERENDUM, LOCAL |
| `Ballot` | The voting document presented to a voter |
| `BallotStyle` | A variant of a ballot (by district, language, or accessibility) |
| `Contest` | A single race or question on a ballot |
| `Candidate` | A person or option in a Contest |
| `VoterRoll` | The list of eligible voters for an election |
| `VotingSession` | A voter's single interaction with a voting system |
| `VotingResult` | Immutable record of cast votes for a Contest |
| `TabulationReport` | Aggregated results report |
| `AuditLog` | Append-only event log for all state changes |
| `ExtensionPack` | A country/region-specific module extending Core |

---

## 6. BPMN Process Registry

| Process ID | File | Type | Status |
|---|---|---|---|
| `election-publication` | `ElectionPublicationProcess.bpmn` | BPMN | Planned |
| `candidate-registration` | `CandidateRegistrationProcess.bpmn` | BPMN | Planned |
| `voter-eligibility-check` | `VoterEligibilityCheck.dmn` | DMN | Planned |
| `ballot-tabulation` | `BallotTabulationProcess.bpmn` | BPMN | Planned |
| `vote-correction` | `VoteCorrectionProcess.bpmn` | BPMN | Planned |
| `result-certification` | `ResultCertificationProcess.bpmn` | BPMN | Planned |
| `audit-review` | `AuditReviewProcess.bpmn` | BPMN | Planned |

---

## 7. Environment Variables Reference

All secrets come from environment variables. Never hardcode.

```
MIREMS_DB_URL           # JDBC URL for PostgreSQL
MIREMS_DB_USER          # DB username
MIREMS_DB_PASS          # DB password
MIREMS_KEYCLOAK_URL     # Keycloak server URL
MIREMS_KEYCLOAK_REALM   # Realm name
MIREMS_KEYCLOAK_CLIENT  # Client ID
MIREMS_KAFKA_BOOTSTRAP  # Kafka bootstrap servers
MIREMS_ENCRYPTION_KEY   # AES-256 key for PII encryption
```

---

## 8. Testing Requirements by Layer

| Layer | Framework | Minimum Coverage |
|---|---|---|
| Domain (Java) | JUnit 5 + AssertJ | 90% |
| Service (Java) | JUnit 5 + Mockito | 80% |
| BPMN Process | Kogito Test Utils | 80% (happy path + exception paths) |
| REST API | Spring Boot Test + RestAssured | 80% |
| Repository | Testcontainers (PostgreSQL) | 70% |
| Frontend Components | Vitest + React Testing Library | 75% |
| E2E | Playwright | Critical paths only |

---

## 9. Extension Pack Development Contract

When creating a new Extension Pack (e.g., `ext-jp` for Japan):

1. Copy `ext-template/` scaffold.
2. Implement `ElectionExtensionPack` interface from `core-domain`.
3. Register via Spring `@ConditionalOnProperty(name="mirems.extension.{code}.enabled")`.
4. Add country-specific DMN rules to `src/main/resources/processes/ext/{code}/`.
5. Add Flyway migration under `src/main/resources/db/migration/ext/{code}/`.
6. Add UI module under `frontend/extensions/ext-{code}-ui/`.
7. Document legal/regulatory basis in `docs/extensions/{code}/LEGAL.md`.

---

## 10. Agent Prompt Template

When starting a new conversation for a specific goal, use this prompt:

```
You are a Senior Full Stack Architect working on MiREMS Platform (Miru Election Management Solution).
Read AGENTS.md and PLAN.md first. Then execute goal: [GOAL_ID].

Stack: Java 21 (Corretto), Spring Boot 3.5.x, Kogito 10.2.0, PostgreSQL 16.4,
       React 19, TypeScript 5.9, TanStack Router, Vite, pnpm.

Constraints:
- NIST VVSG 2.0 compliant
- Hybrid BPMN/Direct service architecture
- Core/Extension Pack separation
- All vote data is immutable after commit
- 80% test coverage gate

Current goal: [GOAL_ID] - [GOAL_TITLE]
Context files: [LIST_CONTEXT_FILES]

Execute all tasks in the goal definition. Report DONE or BLOCKED.
```
