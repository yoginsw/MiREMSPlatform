# MiREMS Platform
### Miru Election Management Solution

> **미루(Miru) 선거 관리 솔루션** — A standards-compliant, extensible election management platform designed to support diverse election types across multiple countries.

[![NIST VVSG 2.0](https://img.shields.io/badge/NIST%20VVSG-2.0%20Compliant-blue)](docs/vvsg/VVSG2_MAPPING.md)
[![Build](https://img.shields.io/github/actions/workflow/status/mirems/mirems-platform/ci.yml)](https://github.com/mirems/mirems-platform)
[![Java](https://img.shields.io/badge/Java-21%20Corretto-red)](https://aws.amazon.com/corretto/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue)](https://react.dev)

---

## Table of Contents

1. [Project Vision](#1-project-vision)
2. [Architecture Overview](#2-architecture-overview)
3. [Technology Stack](#3-technology-stack)
4. [Folder Structure](#4-folder-structure)
5. [Core Module Guide](#5-core-module-guide)
6. [Extension Pack System](#6-extension-pack-system)
7. [Hybrid BPMN/Direct Service Architecture](#7-hybrid-bpmndirect-service-architecture)
8. [VVSG 2.0 Compliance](#8-vvsg-20-compliance)
9. [Getting Started](#9-getting-started)
10. [AI Agent Development Guide](#10-ai-agent-development-guide)
11. [Contributing](#11-contributing)

---

## 1. Project Vision

MiREMS is built on the recognition that elections are among the most consequential processes a society conducts, and that election software must be transparent, auditable, and verifiable to an extraordinary degree. The platform addresses this by treating **every state change as an auditable event**, **every vote record as immutable**, and **every business process as an explicit, inspectable workflow**.

The platform is designed with three long-term goals in mind.

**Multi-country extensibility** means that while every country has different electoral laws, ballot formats, and administrative workflows, the underlying domain model — Elections, Contests, Candidates, Ballots, and VotingResults — is universal. Country-specific rules live in Extension Packs that plug into the Core without modifying it.

**NIST VVSG 2.0 alignment** means the system is designed from the ground up to satisfy the Voluntary Voting System Guidelines 2.0, the most rigorous publicly available standard for election system software. Audit trails, accessibility requirements, data integrity mechanisms, and process transparency all trace back to specific VVSG requirements documented in `docs/vvsg/VVSG2_MAPPING.md`.

**AI-assisted development** means the codebase is structured so that an AI coding agent (Hermes) can incrementally build features over a long period, with each `GOAL` in `PLAN.md` being a self-contained, testable, merge-ready unit of work.

---

## 2. Architecture Overview

MiREMS uses a **Hybrid Architecture** combining Kogito BPMN/DMN workflow processes with traditional Spring Boot Direct Services. The guiding principle for choosing between the two is whether a given operation involves multiple parties, requires human approval steps, needs to be audited as an explicit workflow instance, or spans a significant period of time. If so, it is a BPMN process. Simple CRUD operations, eligibility lookups, and stateless calculations are Direct Services.

```
┌─────────────────────────────────────────────────────────────┐
│                    MiREMS Platform                           │
│                                                             │
│  ┌──────────────────────┐  ┌──────────────────────────────┐ │
│  │   Frontend Shell     │  │      Extension Pack UI       │ │
│  │   React 19 + TSR     │  │   (ext-kr-ui / ext-us-ui)   │ │
│  └──────────┬───────────┘  └──────────────┬───────────────┘ │
│             │ HTTPS / OIDC PKCE            │                 │
│  ┌──────────▼─────────────────────────────▼───────────────┐ │
│  │              Core API Layer (Spring Boot)              │ │
│  │         REST Controllers + OpenAPI 3.1 Spec            │ │
│  └───────────────┬──────────────────┬──────────────────────┘ │
│                  │                  │                         │
│  ┌───────────────▼──────┐  ┌────────▼──────────────────────┐ │
│  │  BPMN/DMN Processes  │  │    Direct Spring Services     │ │
│  │  (Kogito 10.2.0)     │  │  (CRUD / Stateless / Query)  │ │
│  │                      │  │                               │ │
│  │ • ElectionPublication│  │ • ElectionManagementService  │ │
│  │ • CandidateReg       │  │ • VoterRollService           │ │
│  │ • BallotTabulation   │  │ • DistrictService            │ │
│  │ • VoteCorrection     │  │ • BallotStyleService         │ │
│  │ • ResultCertification│  │ • TabulationReportService    │ │
│  └──────────┬───────────┘  └────────┬──────────────────────┘ │
│             │                       │                         │
│  ┌──────────▼───────────────────────▼───────────────────────┐ │
│  │               Core Domain Model                          │ │
│  │  Election · Contest · Candidate · Ballot · VotingResult  │ │
│  │  VoterRoll · VotingSession · AuditLog                   │ │
│  └──────────────────────────┬───────────────────────────────┘ │
│                             │                                  │
│  ┌──────────────────────────▼───────────────────────────────┐ │
│  │              Infrastructure Layer                        │ │
│  │   PostgreSQL 16.4  │  Kafka  │  Keycloak  │  Flyway     │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Extension Packs (Optional)                │  │
│  │   ext-kr (공직선거법)  │  ext-us (HAVA/UOCAVA)         │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

The key architectural insight is the `ProcessService` wrapper. Direct Services that need to trigger a BPMN process never call Kogito directly — they go through `MiremsProcessService`, an interface that abstracts the Kogito API. This means the BPMN engine can be swapped or mocked in tests without touching service logic, and it creates a clean separation between "what to do" (service) and "how to orchestrate it" (process engine).

---

## 3. Technology Stack

### 3.1 Backend

| Component | Technology | Version | Purpose |
|---|---|---|---|
| Runtime | Amazon Corretto JDK | 21 (LTS) | Java runtime with long-term AWS support |
| Framework | Spring Boot | 3.5.x | Application framework, auto-configuration |
| Build | Gradle | 8.10.2 | Multi-project build with BOM platform |
| Process Engine | Kogito | 10.2.0 BOM | BPMN 2.0 and DMN 1.3 process execution |
| Database | PostgreSQL | 16.4 | Primary relational store |
| Migrations | Flyway | (via Spring Boot BOM) | Schema versioning |
| ORM | Spring Data JPA + Hibernate | (via Spring Boot BOM) | Entity mapping and repositories |
| Security | Spring Security + OAuth2 RS | (via Spring Boot BOM) | JWT/OIDC resource server |
| Identity | Keycloak | 24.x | OIDC provider, RBAC |
| Messaging | Apache Kafka | 3.7 | Domain event streaming, audit log |
| API Docs | SpringDoc OpenAPI | 2.x | OpenAPI 3.1 spec generation |
| Mapping | MapStruct | 1.5.5 | DTO ↔ Entity mapping (compile-time) |
| Testing | JUnit 5, Mockito, AssertJ | — | Unit and integration testing |
| Containers | Testcontainers | 1.19.8 | PostgreSQL/Kafka integration tests |

**Important — Kogito BOM Version Alignment.** Kogito 10.2.0 aligns with the `org.kie.kogito:kogito-bom:10.2.0` BOM. All Kogito artifacts (`kogito-spring-boot-starter`, `kogito-processes-spring-boot-starter`, `kogito-decisions-spring-boot-starter`) must be imported through this BOM to guarantee compatibility. Do not declare individual Kogito artifact versions — always use the BOM.

### 3.2 Frontend

| Component | Technology | Version | Purpose |
|---|---|---|---|
| Runtime | Node.js | 22 LTS | JavaScript runtime |
| Package Manager | pnpm | 9.x | Monorepo workspace management |
| Framework | React | 19 | UI component framework |
| Language | TypeScript | 5.9 LTS | Typed JavaScript |
| Build Tool | Vite | 6.x | Fast dev server and build bundler |
| Routing | TanStack Router | latest | Type-safe file-based routing |
| Auth | oidc-client-ts | 3.x | OIDC PKCE auth flow |
| HTTP Client | Axios (generated) | — | OpenAPI-generated typed API client |
| Testing | Vitest + RTL | — | Component unit tests |
| E2E Testing | Playwright | — | Critical path E2E tests |
| Accessibility | axe-core | — | Automated WCAG 2.1 AA checks |

---

## 4. Folder Structure

The repository is a monorepo containing both backend (Gradle multi-project) and frontend (pnpm workspace) codebases.

```
mirems-platform/
│
├── backend/                          # All Java/Kotlin backend code
│   ├── mirems-bom/                   # Gradle platform BOM — single source of truth for all versions
│   │   └── build.gradle.kts
│   │
│   ├── mirems-core/                  # Core platform (country-agnostic)
│   │   ├── core-domain/              # Domain model: Entities, Aggregates, Domain Events, Ports
│   │   │   └── src/main/java/io/mirems/core/domain/
│   │   │       ├── election/         # Election, ElectionStatus, ElectionType
│   │   │       ├── contest/          # Contest, ContestType, Candidate
│   │   │       ├── ballot/           # Ballot, BallotStyle, BallotContest
│   │   │       ├── voter/            # VoterRecord, VotingSession, VotingResult
│   │   │       ├── audit/            # AuditEvent, AuditEventPublisher
│   │   │       └── extension/        # ExtensionPack interface, ExtensionPackRegistry
│   │   │
│   │   ├── core-bpmn/               # Kogito BPMN/DMN processes and adapters
│   │   │   └── src/main/
│   │   │       ├── java/io/mirems/core/bpmn/
│   │   │       │   ├── adapter/      # KogitoProcessAdapter, MiremsProcessService interface
│   │   │       │   ├── handler/      # @KogitoWorkItemHandler implementations
│   │   │       │   └── service/      # Process-specific service beans
│   │   │       └── resources/processes/
│   │   │           ├── ElectionPublicationProcess.bpmn
│   │   │           ├── CandidateRegistrationProcess.bpmn
│   │   │           ├── BallotTabulationProcess.bpmn
│   │   │           ├── VoteCorrectionProcess.bpmn
│   │   │           ├── ResultCertificationProcess.bpmn
│   │   │           ├── AuditReviewProcess.bpmn
│   │   │           └── dmn/
│   │   │               ├── VoterEligibilityCheck.dmn
│   │   │               └── CandidateEligibilityCheck.dmn
│   │   │
│   │   ├── core-api/                # Spring REST controllers, OpenAPI spec
│   │   │   └── src/main/java/io/mirems/core/api/
│   │   │       ├── controller/       # ElectionController, BallotController, VotingController, ...
│   │   │       ├── dto/              # Request/Response records (generated from OpenAPI spec)
│   │   │       └── security/         # SecurityConfig, MiremsSecurityContext, @ElectionScoped
│   │   │
│   │   └── core-infra/              # Infrastructure adapters (JPA, Flyway, Kafka, Keycloak)
│   │       └── src/main/
│   │           ├── java/io/mirems/core/infra/
│   │           │   ├── jpa/          # JPA Repositories, PiiEncryptionConverter, @AuditAction AOP
│   │           │   ├── kafka/        # Kafka producers and consumers
│   │           │   └── config/       # DataSourceConfig, SecurityBeans, LoggingConfig
│   │           └── resources/
│   │               ├── db/migration/ # Flyway SQL scripts (V1__ through Vn__)
│   │               └── application.yml
│   │
│   ├── mirems-auth/                  # Keycloak integration utilities and RBAC helpers
│   │
│   └── extensions/                   # Extension Packs (depend on Core; Core never imports these)
│       ├── ext-template/             # Scaffold for new country packs — copy this to start
│       ├── ext-common/               # Shared utilities used by multiple extension packs
│       ├── ext-kr/                   # 대한민국 선거 확장팩
│       │   └── src/main/
│       │       ├── java/io/mirems/ext/kr/
│       │       │   ├── domain/       # KrJurisdiction, KrElectionType enums
│       │       │   ├── service/      # Proportional representation tabulation, early voting
│       │       │   └── config/       # KrElectionExtensionPack @ConditionalOnProperty bean
│       │       └── resources/
│       │           ├── processes/ext/kr/
│       │           │   ├── KrCandidateEligibility.dmn
│       │           │   ├── KrVoterEligibility.dmn
│       │           │   └── KrCampaignPeriod.dmn
│       │           └── db/migration/ext/kr/
│       │               └── V100__kr_extension_tables.sql
│       └── ext-us/                   # United States election extension pack
│           └── src/main/
│               ├── java/io/mirems/ext/us/
│               │   ├── domain/       # UsJurisdiction (State/County/Precinct), FIPS codes
│               │   ├── service/      # RCV instant-runoff tabulation, absentee tracking
│               │   └── config/       # UsElectionExtensionPack @ConditionalOnProperty bean
│               └── resources/
│                   ├── processes/ext/us/
│                   │   ├── UsVoterEligibility.dmn
│                   │   └── UsAbsenteeBallot.dmn
│                   └── db/migration/ext/us/
│                       └── V200__us_extension_tables.sql
│
├── frontend/                         # All frontend code (pnpm workspace)
│   ├── pnpm-workspace.yaml
│   ├── package.json                  # Root workspace package (scripts, engines)
│   ├── tsconfig.base.json            # Shared strict TypeScript config
│   ├── .eslintrc.cjs                 # Shared ESLint + Prettier config
│   │
│   ├── mirems-shell/                 # Main application shell (Vite + React 19 + TanStack Router)
│   │   ├── src/
│   │   │   ├── routes/               # TanStack Router file-based routes
│   │   │   │   ├── __root.tsx        # Root layout (auth check, nav shell)
│   │   │   │   ├── index.tsx         # Landing page (public)
│   │   │   │   ├── elections/
│   │   │   │   │   ├── index.tsx     # Election list
│   │   │   │   │   ├── new.tsx       # Creation wizard
│   │   │   │   │   └── $id/
│   │   │   │   │       ├── index.tsx # Election detail
│   │   │   │   │       ├── contests.tsx
│   │   │   │   │       ├── ballots.tsx
│   │   │   │   │       └── results.tsx
│   │   │   │   ├── vote/
│   │   │   │   │   └── $sessionId.tsx # Kiosk voting UI
│   │   │   │   ├── audit.tsx
│   │   │   │   └── admin/
│   │   │   │       └── index.tsx
│   │   │   ├── auth/                 # AuthProvider, ProtectedRoute, useAuth hook
│   │   │   ├── components/           # Shell-specific components (Navbar, Sidebar, ErrorBoundary)
│   │   │   └── main.tsx
│   │   ├── vite.config.ts
│   │   └── package.json
│   │
│   ├── packages/                     # Shared internal packages
│   │   ├── ui-core/                  # Component library: Button, Table, Modal, Input, ...
│   │   │   ├── src/components/
│   │   │   └── package.json
│   │   ├── api-client/               # OpenAPI-generated TypeScript Axios client
│   │   │   ├── src/generated/        # AUTO-GENERATED — do not edit manually
│   │   │   └── package.json
│   │   └── i18n/                     # Shared translation keys and locale files
│   │       ├── locales/
│   │       │   ├── en.json
│   │       │   └── ko.json
│   │       └── package.json
│   │
│   └── extensions/                   # Country-specific UI extension modules
│       ├── ext-kr-ui/                # 한국 선거 UI (비례대표 ballot, 사전투표 UI)
│       └── ext-us-ui/                # US election UI (RCV ballot, absentee tracking)
│
├── infrastructure/
│   ├── docker/
│   │   ├── docker-compose.dev.yml    # Local dev: PostgreSQL, Keycloak, Kafka, Kogito console
│   │   ├── docker-compose.test.yml   # CI: same services, ephemeral volumes
│   │   └── .env.example             # Required env var template
│   └── k8s/
│       └── helm/
│           ├── mirems-core/          # Core platform Helm chart
│           └── mirems-ext-kr/        # KR extension pack chart
│
├── docs/
│   ├── adr/                          # Architecture Decision Records (ADR-001 through ADR-006)
│   ├── vvsg/
│   │   └── VVSG2_MAPPING.md         # NIST VVSG 2.0 requirement traceability matrix
│   ├── auth/
│   │   └── ROLE_MATRIX.md           # Role → permission matrix for all endpoints
│   ├── api/
│   │   └── mirems-api.yaml          # Master OpenAPI 3.1 specification
│   └── extensions/
│       ├── kr/LEGAL.md              # 공직선거법 references for ext-kr
│       └── us/LEGAL.md              # HAVA/UOCAVA references for ext-us
│
├── Makefile                          # Developer convenience targets
├── AGENTS.md                         # AI agent instruction file
├── PLAN.md                           # Phased development plan with goal definitions
└── README.md                         # This file
```

---

## 5. Core Module Guide

### 5.1 core-domain

This module contains only domain logic and has zero infrastructure dependencies — no Spring, no JPA, no Kafka. It defines the entities, aggregates, domain events, and service port interfaces. The key aggregates are `Election` (which owns its state machine and enforces all lifecycle transitions) and `VotingResult` (which is immutable from the moment it is created).

Every aggregate that changes state emits a corresponding domain event. These events are published through `AuditEventPublisher` after the enclosing transaction commits, ensuring consistency between the database state and the event stream.

### 5.2 core-bpmn

This module owns all Kogito BPMN and DMN files and their Java work item handlers. The most important class here is `KogitoProcessAdapter`, which implements `MiremsProcessService`. Any code outside this module that needs to interact with a BPMN process must go through this interface — never through the Kogito API directly. This design enables straightforward mocking in unit tests and clear boundary enforcement.

### 5.3 core-api

This module contains the Spring MVC REST controllers. Controllers are thin — they receive a request, validate it, call a service or process, and return a mapped response. They never contain business logic. All controllers implement the stubs generated from `docs/api/mirems-api.yaml`, which means the API contract is the source of truth, not the controller signatures.

### 5.4 core-infra

This module contains everything that touches the outside world: JPA repositories and their implementations, Flyway configuration, Kafka producer/consumer beans, and Keycloak adapter configuration. The `@AuditAction` AOP annotation is defined here. It intercepts any `@Service` method annotated with it and automatically creates an `AuditEvent` after successful completion, making audit logging opt-in rather than manual.

---

## 6. Extension Pack System

An Extension Pack is a Spring Boot auto-configuration module that activates via a property flag. The Core never imports Extension Pack code — the dependency arrow always points from Extension toward Core. This is enforced by Gradle's project dependency declarations.

To understand how an extension integrates, consider the lifecycle of adding a new country pack. You start by copying `ext-template`, which provides the `ElectionExtensionPack` interface implementation, an empty Flyway migration for country-specific tables, and a `@ConditionalOnProperty` configuration class. The Spring `ExtensionPackRegistry` in Core discovers the pack at startup via the interface. Country-specific DMN files declare rules that override or supplement the base `VoterEligibilityCheck.dmn`. Country-specific BPMN sub-processes can be attached to Core processes via BPMN compensation events or as standalone processes triggered by domain events.

The UI side mirrors this structure. Each `ext-{code}-ui` pnpm package exports route segments and components that the `mirems-shell` dynamically incorporates when the extension is enabled, using a plugin registration pattern compatible with TanStack Router.

---

## 7. Hybrid BPMN/Direct Service Architecture

The decision of when to use BPMN versus a Direct Service is one of the most important architectural choices in MiREMS. The rule is stated precisely in `docs/adr/ADR-002-hybrid-bpmn-direct.md`, but the intuition is this: if a human being needs to be involved, if the operation takes more than one database transaction, or if you need to be able to look at an audit trail and reconstruct exactly which step failed and why — it's a BPMN process. Everything else is a Direct Service.

A practical example illustrates the distinction. When an election administrator updates a district name, that is a Direct Service call — it is a single transaction, it is performed by one party, and it completes immediately. When an election administrator wants to publish an election, that triggers `ElectionPublicationProcess.bpmn` — it involves a review step by a second administrator, validates that all contests and ballot styles are properly configured, and only then transitions the election to PUBLISHED status. The process instance is persisted by Kogito, so if the system restarts mid-process, it resumes exactly where it left off.

---

## 8. VVSG 2.0 Compliance

MiREMS is designed to align with the NIST Voluntary Voting System Guidelines 2.0. The most significant implementation choices driven by VVSG 2.0 are listed below.

**Vote record immutability** (maps to VVSG 2.0 Vol. I §3.3) is implemented by making `VotingResult` a fully immutable JPA entity with no setters and no update operations in its repository. Administrative corrections go through a separate `VoteCorrection` entity and a dual-approval BPMN process, leaving the original record untouched.

**Audit trail completeness** (maps to VVSG 2.0 Vol. I §12) is addressed by the `@AuditAction` annotation and `AuditEventPublisher`, which together ensure that every state-changing operation on election data produces an `AuditEvent` record in an append-only table and publishes to the Kafka audit topic. This audit log is queryable by auditors through a dedicated restricted API.

**Voter privacy and PII protection** is handled by encrypting `VoterRecord.externalVoterId` with AES-256 at the JPA converter level before it is ever written to the database. Response DTOs never expose raw voter IDs.

**Accessibility** (maps to VVSG 2.0 Vol. I §4) requires WCAG 2.1 Level AA compliance for any voter-facing UI. The voting kiosk page (`VotingSessionPage`) is built with large targets, high-contrast themes, full keyboard navigation, and ARIA roles, and is verified against WCAG criteria using axe-core in the test suite.

The complete traceability matrix mapping every VVSG 2.0 requirement to a MiREMS module or feature is maintained in `docs/vvsg/VVSG2_MAPPING.md`.

---

## 9. Getting Started

### Prerequisites

Make sure you have the following installed before starting.

- Docker Desktop (or equivalent) for running local infrastructure
- Amazon Corretto JDK 21
- Gradle 8.10.2 (or use the Gradle wrapper: `./gradlew`)
- Node.js 22 LTS
- pnpm 9 (`npm install -g pnpm`)
- Make (for Makefile convenience targets)

### Local Development Setup

Clone the repository and copy the environment variable template.

```bash
git clone https://github.com/mirems/mirems-platform.git
cd mirems-platform
cp infrastructure/docker/.env.example infrastructure/docker/.env
# Edit .env and fill in MIREMS_ENCRYPTION_KEY and other secrets
```

Start the infrastructure services (PostgreSQL, Keycloak, Kafka).

```bash
make dev-up
```

Build and run the backend. Flyway migrations will run automatically on startup.

```bash
./gradlew :backend:mirems-core:core-api:bootRun
```

In a separate terminal, install frontend dependencies and start the dev server.

```bash
cd frontend
pnpm install
pnpm -r build          # Build shared packages first
cd mirems-shell
pnpm dev               # Vite dev server on http://localhost:5173
```

### Running Tests

The backend tests use Testcontainers and require Docker to be running.

```bash
./gradlew test                    # All backend tests
./gradlew jacocoTestReport        # Generate coverage report
cd frontend && pnpm -r test       # All frontend tests
```

### Enabling Extension Packs

To enable the Korean election extension pack, add to `application.yml` or your environment:

```yaml
mirems:
  extension:
    kr:
      enabled: true
    us:
      enabled: false
```

---

## 10. AI Agent Development Guide

MiREMS is designed to be incrementally built by an AI coding agent (Hermes) over many sessions. The following principles make this sustainable.

**One goal at a time.** Each `GOAL` in `PLAN.md` is designed to be completable in a single agent session of 30–60 minutes. Goals have explicit task lists, done criteria, and dependency declarations. An agent should never start a goal without verifying all its dependencies are `DONE`.

**Context-first discipline.** Before writing any code, the agent reads the `CONTEXT files` listed in the goal. This prevents the agent from making architectural decisions that contradict already-recorded ADRs or VVSG mappings.

**Test gate is non-negotiable.** A goal is not `DONE` until the tests pass and coverage meets the threshold. The CI pipeline enforces this, but the agent should self-verify before marking a goal complete.

**Commit message format** encodes traceability: `[GOAL-P1-009] feat: Add Election aggregate with state machine`. This allows the entire development history to be traced back to specific plan items.

To start an agent session for a specific goal, use the prompt template in `AGENTS.md §10`, substituting the goal ID and listing the context files. The agent will then read this README for the big picture, read `AGENTS.md` for constraints, and execute the tasks in `PLAN.md`.

---

## 11. Contributing

All code contributions go through the goal system in `PLAN.md`. Human contributors should mark a goal `IN_PROGRESS` before starting work, and submit a pull request referencing the goal ID. The PR description should confirm that all done criteria are met and that the CI pipeline passes.

For changes that affect the VVSG 2.0 compliance posture, `docs/vvsg/VVSG2_MAPPING.md` must be updated in the same PR. For new ADRs, follow the format in `docs/adr/ADR-000-template.md`.

Extension pack contributions must comply with the Extension Pack Development Contract defined in `AGENTS.md §9`.

---

*MiREMS Platform — Building trustworthy elections, one process at a time.*
