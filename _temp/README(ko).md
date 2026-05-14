# MiREMS(Miru Election Management Solution) Platform

### MiREMS Platform 개요

> **미루(Miru) 선거 관리 솔루션** — 여러 국가의 다양한 선거 유형을 지원하도록 설계된, 표준을 준수하고 확장이 가능한 선거 관리 플랫폼입니다.

[![NIST VVSG 2.0](https://img.shields.io/badge/NIST%20VVSG-2.0%20Compliant-blue)](docs/vvsg/VVSG2_MAPPING.md)
[![Build](https://img.shields.io/github/actions/workflow/status/mirems/mirems-platform/ci.yml)](https://github.com/mirems/mirems-platform)
[![Java](https://img.shields.io/badge/Java-21%20Corretto-red)](https://aws.amazon.com/corretto/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue)](https://react.dev)

---

## 목차

1. [프로젝트 비전](#1-프로젝트-비전)
2. [아키텍처 개요](#2-아키텍처-개요)
3. [기술 스택](#3-기술-스택)
4. [폴더 구조](#4-폴더-구조)
5. [코어 모듈 가이드](#5-코어-모듈-가이드)
6. [확장팩 시스템](#6-확장팩-시스템)
7. [하이브리드 BPMN/Direct Service 아키텍처](#7-하이브리드-bpmndirect-service-아키텍처)
8. [VVSG 2.0 규정 준수](#8-vvsg-20-규정-준수)
9. [시작하기](#9-시작하기)
10. [AI 에이전트 개발 가이드](#10-ai-에이전트-개발-가이드)
11. [기여하기](#11-기여하기)

---

## 1. 프로젝트 비전

MiREMS는 선거가 사회가 수행하는 가장 중요한 프로세스 중 하나이며, 선거 소프트웨어는 엄청난 수준의 투명성, 감사 가능성, 검증 가능성을 갖춰야 한다는 인식 위에 구축되었습니다. 이 플랫폼은 **모든 상태 변경을 감사 가능한 이벤트로**, **모든 투표 기록을 불변으로**, **모든 비즈니스 프로세스를 명시적이고 검사 가능한 워크플로우로** 취급하여 이를 해결합니다.

플랫폼은 다음 세 가지 장기적인 목표를 염두에 두고 설계되었습니다.

**다국가 확장성(Multi-country extensibility)**은 모든 국가가 서로 다른 선거법, 투표용지 형식, 행정 워크플로우를 가지고 있지만 근본적인 도메인 모델(선거, 경선, 후보자, 투표용지, 투표 결과)은 보편적이라는 것을 의미합니다. 국가별 규칙은 코어(Core)를 수정하지 않고 연결되는 확장팩(Extension Packs)에 존재합니다.

**NIST VVSG 2.0 준수(NIST VVSG 2.0 alignment)**는 시스템이 선거 시스템 소프트웨어에 대해 공개적으로 이용 가능한 가장 엄격한 표준인 자발적 투표 시스템 가이드라인 2.0(Voluntary Voting System Guidelines 2.0)을 만족하도록 처음부터 설계되었음을 의미합니다. 감사 추적, 접근성 요구사항, 데이터 무결성 메커니즘 및 프로세스 투명성은 모두 `docs/vvsg/VVSG2_MAPPING.md`에 문서화된 특정 VVSG 요구사항을 따릅니다.

**AI 지원 개발(AI-assisted development)**은 코드베이스가 AI 코딩 에이전트(Hermes)가 장기간에 걸쳐 점진적으로 기능을 구축할 수 있도록 구조화되어 있으며, `PLAN.md`의 각 `GOAL`이 독립적이고 테스트 가능하며 병합 가능한 작업 단위가 됨을 의미합니다.

---

## 2. 아키텍처 개요

MiREMS는 Kogito BPMN/DMN 워크플로우 프로세스와 전통적인 Spring Boot Direct Service를 결합한 **하이브리드 아키텍처**를 사용합니다. 두 가지 중 하나를 선택하는 기본 원칙은 특정 작업이 다수의 이해관계자를 포함하는지, 인간의 승인 단계가 필요한지, 명시적인 워크플로우 인스턴스로 감사되어야 하는지, 아니면 상당한 기간에 걸쳐 발생하는지 여부입니다. 만약 그렇다면 BPMN 프로세스입니다. 단순한 CRUD 작업, 적격성 조회 및 상태 비저장(stateless) 계산은 Direct Service입니다.

```text
┌─────────────────────────────────────────────────────────────┐
│                    MiREMS Platform                          │
│                                                             │
│  ┌──────────────────────┐  ┌──────────────────────────────┐ │
│  │   Frontend Shell     │  │      Extension Pack UI       │ │
│  │   React 19 + TSR     │  │   (ext-kr-ui / ext-us-ui)    │ │
│  └──────────┬───────────┘  └──────────────┬───────────────┘ │
│             │ HTTPS / OIDC PKCE           │                 │
│  ┌──────────▼─────────────────────────────▼───────────────┐ │
│  │              코어 API 계층 (Spring Boot)                │ │
│  │         REST Controllers + OpenAPI 3.1 Spec            │ │
│  └───────────────┬──────────────────┬─────────────────────┘ │
│                  │                  │                       │
│  ┌───────────────▼──────┐  ┌────────▼─────────────────────┐ │
│  │  BPMN/DMN 프로세스    │  │    Direct Spring 서비스       │ │
│  │  (Kogito 10.2.0)     │  │  (CRUD / Stateless / Query)  │ │
│  │                      │  │                              │ │
│  │ • ElectionPublication│  │ • ElectionManagementService  │ │
│  │ • CandidateReg       │  │ • VoterRollService           │ │
│  │ • BallotTabulation   │  │ • DistrictService            │ │
│  │ • VoteCorrection     │  │ • BallotStyleService         │ │
│  │ • ResultCertification│  │ • TabulationReportService    │ │
│  └──────────┬───────────┘  └────────┬─────────────────────┘ │
│             │                       │                       │
│  ┌──────────▼───────────────────────▼─────────────────────┐ │
│  │                     코어 도메인 모델                     │ │
│  │  Election · Contest · Candidate · Ballot · VotingResult│ │
│  │  VoterRoll · VotingSession · AuditLog                  │ │
│  └──────────────────────────┬─────────────────────────────┘ │
│                             │                               │
│  ┌──────────────────────────▼─────────────────────────────┐ │
│  │                     인프라 계층                          │ │
│  │   PostgreSQL 16.4  │  Kafka  │  Keycloak  │  Flyway    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                  확장팩 (선택 사항)                      │ │
│  │   ext-kr (공직선거법)  │  ext-us (HAVA/UOCAVA)         │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

```

핵심적인 아키텍처의 통찰은 `ProcessService` 래퍼(wrapper)에 있습니다. BPMN 프로세스를 트리거해야 하는 Direct Service는 Kogito를 직접 호출하지 않고, Kogito API를 추상화한 인터페이스인 `MiremsProcessService`를 거칩니다. 이는 서비스 로직을 건드리지 않고도 단위 테스트에서 BPMN 엔진을 교체하거나 모킹(mocking)할 수 있음을 의미하며, "무엇을 할 것인가"(서비스)와 "그것을 어떻게 오케스트레이션할 것인가"(프로세스 엔진)를 명확하게 분리합니다.

---

## 3. 기술 스택

### 3.1 백엔드

| 구성 요소 | 기술 | 버전 | 목적 |
| --- | --- | --- | --- |
| 런타임 | Amazon Corretto JDK | 21 (LTS) | 장기 AWS 지원이 포함된 Java 런타임 |
| 프레임워크 | Spring Boot | 3.5.x | 애플리케이션 프레임워크, 자동 구성 |
| 빌드 | Gradle | 8.10.2 | BOM 플랫폼이 포함된 멀티 프로젝트 빌드 |
| 프로세스 엔진 | Kogito | 10.2.0 BOM | BPMN 2.0 및 DMN 1.3 프로세스 실행 |
| 데이터베이스 | PostgreSQL | 16.4 | 기본 관계형 저장소 |
| 마이그레이션 | Flyway | (Spring Boot BOM) | 스키마 버전 관리 |
| ORM | Spring Data JPA + Hibernate | (Spring Boot BOM) | 엔티티 매핑 및 리포지토리 |
| 보안 | Spring Security + OAuth2 RS | (Spring Boot BOM) | JWT/OIDC 리소스 서버 |
| 인증/인가 | Keycloak | 24.x | OIDC 프로바이더, RBAC |
| 메시징 | Apache Kafka | 3.7 | 도메인 이벤트 스트리밍, 감사 로그 |
| API 문서 | SpringDoc OpenAPI | 2.x | OpenAPI 3.1 스펙 생성 |
| 매핑 | MapStruct | 1.5.5 | DTO ↔ 엔티티 매핑 (컴파일 타임) |
| 테스트 | JUnit 5, Mockito, AssertJ | — | 단위 및 통합 테스트 |
| 컨테이너 | Testcontainers | 1.19.8 | PostgreSQL/Kafka 통합 테스트 |

**중요 — Kogito BOM 버전 정렬.** Kogito 10.2.0은 `org.kie.kogito:kogito-bom:10.2.0` BOM과 정렬됩니다. 호환성을 보장하려면 모든 Kogito 아티팩트(`kogito-spring-boot-starter`, `kogito-processes-spring-boot-starter`, `kogito-decisions-spring-boot-starter`)를 이 BOM을 통해 가져와야 합니다. 개별 Kogito 아티팩트 버전을 선언하지 말고 항상 BOM을 사용하십시오.

### 3.2 프론트엔드

| 구성 요소 | 기술 | 버전 | 목적 |
| --- | --- | --- | --- |
| 런타임 | Node.js | 22 LTS | JavaScript 런타임 |
| 패키지 관리자 | pnpm | 9.x | 모노레포 워크스페이스 관리 |
| 프레임워크 | React | 19 | UI 컴포넌트 프레임워크 |
| 언어 | TypeScript | 5.9 LTS | 타입이 지정된 JavaScript |
| 빌드 도구 | Vite | 6.x | 빠른 개발 서버 및 빌드 번들러 |
| 라우팅 | TanStack Router | latest | 타입 안전한 파일 기반 라우팅 |
| 인증 | oidc-client-ts | 3.x | OIDC PKCE 인증 흐름 |
| HTTP 클라이언트 | Axios (생성됨) | — | OpenAPI 기반 자동 생성 타입 API 클라이언트 |
| 테스트 | Vitest + RTL | — | 컴포넌트 단위 테스트 |
| E2E 테스트 | Playwright | — | 핵심 경로(Critical path) E2E 테스트 |
| 접근성 | axe-core | — | 자동화된 WCAG 2.1 AA 검사 |

---

## 4. 폴더 구조

리포지토리는 백엔드(Gradle 멀티 프로젝트)와 프론트엔드(pnpm 워크스페이스) 코드베이스를 모두 포함하는 모노레포입니다.

```text
mirems-platform/
│
├── backend/                          # 모든 Java/Kotlin 백엔드 코드
│   ├── mirems-bom/                   # Gradle 플랫폼 BOM — 모든 버전에 대한 단일 진실의 원천
│   │   └── build.gradle.kts
│   │
│   ├── mirems-core/                  # 코어 플랫폼 (국가에 종속되지 않음)
│   │   ├── core-domain/              # 도메인 모델: Entities, Aggregates, Domain Events, Ports
│   │   │   └── src/main/java/io/mirems/core/domain/
│   │   │       ├── election/         # Election, ElectionStatus, ElectionType
│   │   │       ├── contest/          # Contest, ContestType, Candidate
│   │   │       ├── ballot/           # Ballot, BallotStyle, BallotContest
│   │   │       ├── voter/            # VoterRecord, VotingSession, VotingResult
│   │   │       ├── audit/            # AuditEvent, AuditEventPublisher
│   │   │       └── extension/        # ExtensionPack 인터페이스, ExtensionPackRegistry
│   │   │
│   │   ├── core-bpmn/               # Kogito BPMN/DMN 프로세스 및 어댑터
│   │   │   └── src/main/
│   │   │       ├── java/io/mirems/core/bpmn/
│   │   │       │   ├── adapter/      # KogitoProcessAdapter, MiremsProcessService 인터페이스
│   │   │       │   ├── handler/      # @KogitoWorkItemHandler 구현체
│   │   │       │   └── service/      # 프로세스별 서비스 빈(beans)
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
│   │   ├── core-api/                # Spring REST 컨트롤러, OpenAPI 스펙
│   │   │   └── src/main/java/io/mirems/core/api/
│   │   │       ├── controller/       # ElectionController, BallotController, VotingController, ...
│   │   │       ├── dto/              # 요청/응답 records (OpenAPI 스펙에서 생성됨)
│   │   │       └── security/         # SecurityConfig, MiremsSecurityContext, @ElectionScoped
│   │   │
│   │   └── core-infra/              # 인프라 어댑터 (JPA, Flyway, Kafka, Keycloak)
│   │       └── src/main/
│   │           ├── java/io/mirems/core/infra/
│   │           │   ├── jpa/          # JPA Repositories, PiiEncryptionConverter, @AuditAction AOP
│   │           │   ├── kafka/        # Kafka 프로듀서 및 컨슈머
│   │           │   └── config/       # DataSourceConfig, SecurityBeans, LoggingConfig
│   │           └── resources/
│   │               ├── db/migration/ # Flyway SQL 스크립트 (V1__ 부터 Vn__ 까지)
│   │               └── application.yml
│   │
│   ├── mirems-auth/                  # Keycloak 통합 유틸리티 및 RBAC 헬퍼
│   │
│   └── extensions/                   # 확장팩 (코어에 의존함; 코어는 절대 이를 임포트하지 않음)
│       ├── ext-template/             # 새로운 국가 팩을 위한 스캐폴드 — 복사하여 시작
│       ├── ext-common/               # 여러 확장팩에서 사용하는 공통 유틸리티
│       ├── ext-kr/                   # 대한민국 선거 확장팩
│       │   └── src/main/
│       │       ├── java/io/mirems/ext/kr/
│       │       │   ├── domain/       # KrJurisdiction, KrElectionType enums
│       │       │   ├── service/      # 비례대표 집계, 사전투표
│       │       │   └── config/       # KrElectionExtensionPack @ConditionalOnProperty 빈
│       │       └── resources/
│       │           ├── processes/ext/kr/
│       │           │   ├── KrCandidateEligibility.dmn
│       │           │   ├── KrVoterEligibility.dmn
│       │           │   └── KrCampaignPeriod.dmn
│       │           └── db/migration/ext/kr/
│       │               └── V100__kr_extension_tables.sql
│       └── ext-us/                   # 미국 선거 확장팩
│           └── src/main/
│               ├── java/io/mirems/ext/us/
│               │   ├── domain/       # UsJurisdiction (State/County/Precinct), FIPS 코드
│               │   ├── service/      # RCV 즉석 결선 집계, 부재자 추적
│               │   └── config/       # UsElectionExtensionPack @ConditionalOnProperty 빈
│               └── resources/
│                   ├── processes/ext/us/
│                   │   ├── UsVoterEligibility.dmn
│                   │   └── UsAbsenteeBallot.dmn
│                   └── db/migration/ext/us/
│                       └── V200__us_extension_tables.sql
│
├── frontend/                         # 모든 프론트엔드 코드 (pnpm 워크스페이스)
│   ├── pnpm-workspace.yaml
│   ├── package.json                  # 루트 워크스페이스 패키지 (스크립트, 엔진)
│   ├── tsconfig.base.json            # 공유되는 엄격한 TypeScript 구성
│   ├── .eslintrc.cjs                 # 공유되는 ESLint + Prettier 구성
│   │
│   ├── mirems-shell/                 # 메인 애플리케이션 셸 (Vite + React 19 + TanStack Router)
│   │   ├── src/
│   │   │   ├── routes/               # TanStack Router 파일 기반 라우트
│   │   │   │   ├── __root.tsx        # 루트 레이아웃 (인증 확인, 네비게이션 셸)
│   │   │   │   ├── index.tsx         # 랜딩 페이지 (공개)
│   │   │   │   ├── elections/
│   │   │   │   │   ├── index.tsx     # 선거 목록
│   │   │   │   │   ├── new.tsx       # 생성 마법사
│   │   │   │   │   └── $id/
│   │   │   │   │       ├── index.tsx # 선거 세부 정보
│   │   │   │   │       ├── contests.tsx
│   │   │   │   │       ├── ballots.tsx
│   │   │   │   │       └── results.tsx
│   │   │   │   ├── vote/
│   │   │   │   │   └── $sessionId.tsx # 키오스크 투표 UI
│   │   │   │   ├── audit.tsx
│   │   │   │   └── admin/
│   │   │   │       └── index.tsx
│   │   │   ├── auth/                 # AuthProvider, ProtectedRoute, useAuth 훅
│   │   │   ├── components/           # 셸 전용 컴포넌트 (Navbar, Sidebar, ErrorBoundary)
│   │   │   └── main.tsx
│   │   ├── vite.config.ts
│   │   └── package.json
│   │
│   ├── packages/                     # 공유 내부 패키지
│   │   ├── ui-core/                  # 컴포넌트 라이브러리: Button, Table, Modal, Input, ...
│   │   │   ├── src/components/
│   │   │   └── package.json
│   │   ├── api-client/               # OpenAPI 기반 자동 생성 TypeScript Axios 클라이언트
│   │   │   ├── src/generated/        # 자동 생성됨 — 수동으로 편집하지 마세요
│   │   │   └── package.json
│   │   └── i18n/                     # 공유 번역 키 및 로케일 파일
│   │       ├── locales/
│   │       │   ├── en.json
│   │       │   └── ko.json
│   │       └── package.json
│   │
│   └── extensions/                   # 국가별 UI 확장 모듈
│       ├── ext-kr-ui/                # 한국 선거 UI (비례대표 투표용지, 사전투표 UI)
│       └── ext-us-ui/                # 미국 선거 UI (RCV 투표용지, 부재자 추적)
│
├── infrastructure/
│   ├── docker/
│   │   ├── docker-compose.dev.yml    # 로컬 개발용: PostgreSQL, Keycloak, Kafka, Kogito console
│   │   ├── docker-compose.test.yml   # CI용: 동일 서비스, 임시 볼륨
│   │   └── .env.example             # 필수 환경 변수 템플릿
│   └── k8s/
│       └── helm/
│           ├── mirems-core/          # 코어 플랫폼 Helm 차트
│           └── mirems-ext-kr/        # 한국 확장팩 차트
│
├── docs/
│   ├── adr/                          # 아키텍처 결정 기록 (ADR-001 부터 ADR-006)
│   ├── vvsg/
│   │   └── VVSG2_MAPPING.md         # NIST VVSG 2.0 요구사항 추적 매트릭스
│   ├── auth/
│   │   └── ROLE_MATRIX.md           # 역할 → 모든 엔드포인트에 대한 권한 매트릭스
│   ├── api/
│   │   └── mirems-api.yaml          # 마스터 OpenAPI 3.1 명세서
│   └── extensions/
│       ├── kr/LEGAL.md              # ext-kr을 위한 공직선거법 참조
│       └── us/LEGAL.md              # ext-us를 위한 HAVA/UOCAVA 참조
│
├── Makefile                          # 개발자 편의 타겟
├── AGENTS.md                         # AI 에이전트 지침 파일
├── PLAN.md                           # 목표 정의가 포함된 단계별 개발 계획
└── README.md                         # 현재 파일

```

---

## 5. 코어 모듈 가이드

### 5.1 core-domain

이 모듈은 도메인 로직만을 포함하며 인프라 종속성(Spring, JPA, Kafka 등)이 전혀 없습니다. 엔티티(Entities), 애그리거트(Aggregates), 도메인 이벤트(Domain Events) 및 서비스 포트(Service Port) 인터페이스를 정의합니다. 핵심 애그리거트는 `Election`(자체의 상태 머신을 소유하고 모든 수명 주기 전환을 강제함)과 `VotingResult`(생성되는 순간부터 불변임)입니다.

상태가 변경되는 모든 애그리거트는 해당 도메인 이벤트를 발생시킵니다. 이러한 이벤트는 트랜잭션이 커밋된 후 `AuditEventPublisher`를 통해 발행되어 데이터베이스 상태와 이벤트 스트림 간의 일관성을 보장합니다.

### 5.2 core-bpmn

이 모듈은 모든 Kogito BPMN 및 DMN 파일과 해당 Java 작업 항목 핸들러(work item handlers)를 소유합니다. 여기서 가장 중요한 클래스는 `MiremsProcessService`를 구현하는 `KogitoProcessAdapter`입니다. BPMN 프로세스와 상호 작용해야 하는 이 모듈 외부의 모든 코드는 반드시 이 인터페이스를 거쳐야 하며, 절대 Kogito API를 직접 통하지 않습니다. 이러한 설계는 단위 테스트에서의 간단한 모킹(mocking)과 명확한 경계 준수를 가능하게 합니다.

### 5.3 core-api

이 모듈은 Spring MVC REST 컨트롤러를 포함합니다. 컨트롤러는 얇게(thin) 유지됩니다. 즉, 요청을 받고, 유효성을 검사하고, 서비스나 프로세스를 호출하고, 매핑된 응답을 반환합니다. 비즈니스 로직을 포함하지 않습니다. 모든 컨트롤러는 `docs/api/mirems-api.yaml`에서 생성된 스텁(stubs)을 구현하므로, 컨트롤러의 시그니처가 아닌 API 계약(contract)이 진실의 원천(source of truth)이 됩니다.

### 5.4 core-infra

이 모듈은 JPA 리포지토리 및 구현체, Flyway 구성, Kafka 프로듀서/컨슈머 빈(beans), Keycloak 어댑터 구성 등 외부 세계와 닿는 모든 것을 포함합니다. `@AuditAction` AOP 어노테이션이 여기서 정의됩니다. 이 어노테이션이 적용된 `@Service` 메서드를 가로채서 성공적인 완료 후 자동으로 `AuditEvent`를 생성하여, 감사 로깅을 수동이 아닌 자동(opt-in)으로 만듭니다.

---

## 6. 확장팩 시스템

확장팩(Extension Pack)은 속성 플래그(property flag)를 통해 활성화되는 Spring Boot 자동 구성(auto-configuration) 모듈입니다. 코어(Core)는 확장팩 코드를 절대 가져오지(import) 않습니다 — 의존성 방향은 항상 확장팩에서 코어를 향합니다. 이는 Gradle의 프로젝트 의존성 선언에 의해 강제됩니다.

확장팩이 통합되는 방식을 이해하려면 새로운 국가 팩을 추가하는 수명 주기를 고려해 보세요. `ElectionExtensionPack` 인터페이스 구현, 국가별 테이블을 위한 빈 Flyway 마이그레이션, 그리고 `@ConditionalOnProperty` 구성 클래스를 제공하는 `ext-template`을 복사하는 것으로 시작합니다. 코어의 Spring `ExtensionPackRegistry`는 시작 시 인터페이스를 통해 팩을 검색합니다. 국가별 DMN 파일은 기본 `VoterEligibilityCheck.dmn`을 재정의하거나 보완하는 규칙을 선언합니다. 국가별 BPMN 하위 프로세스는 BPMN 보상(compensation) 이벤트를 통해 코어 프로세스에 연결되거나, 도메인 이벤트에 의해 트리거되는 독립형 프로세스로 작동할 수 있습니다.

UI 측면도 이 구조를 반영합니다. 각 `ext-{code}-ui` pnpm 패키지는 확장팩이 활성화될 때 `mirems-shell`이 TanStack Router와 호환되는 플러그인 등록 패턴을 사용하여 동적으로 통합하는 라우트 세그먼트와 컴포넌트를 내보냅니다.

---

## 7. 하이브리드 BPMN/Direct Service 아키텍처

언제 BPMN을 사용하고 언제 Direct Service를 사용할지에 대한 결정은 MiREMS에서 가장 중요한 아키텍처 선택 중 하나입니다. 이 규칙은 `docs/adr/ADR-002-hybrid-bpmn-direct.md`에 명확하게 명시되어 있지만, 직관적으로 설명하자면 다음과 같습니다: 만약 인간이 개입해야 하거나, 작업이 두 개 이상의 데이터베이스 트랜잭션을 소요하거나, 감사 추적을 살펴보고 어떤 단계에서 왜 실패했는지 정확히 재구성해야 한다면 — 그것은 BPMN 프로세스입니다. 그 외의 모든 것은 Direct Service입니다.

실용적인 예가 이 차이를 설명해 줍니다. 선거 관리자가 선거구 이름을 업데이트할 때, 이는 Direct Service 호출입니다. 단일 트랜잭션이며, 단일 당사자에 의해 수행되고 즉시 완료됩니다. 반면 선거 관리자가 선거를 발행(publish)하고자 할 때는 `ElectionPublicationProcess.bpmn`이 트리거됩니다. 이는 두 번째 관리자의 검토 단계를 포함하고, 모든 경선(contests)과 투표용지 형식(ballot styles)이 제대로 구성되었는지 검증한 후에야 선거를 PUBLISHED 상태로 전환합니다. 프로세스 인스턴스는 Kogito에 의해 유지(persist)되므로, 프로세스 도중 시스템이 재시작되더라도 중단된 지점에서 정확히 다시 시작됩니다.

---

## 8. VVSG 2.0 규정 준수

MiREMS는 NIST 자발적 투표 시스템 가이드라인 2.0(Voluntary Voting System Guidelines 2.0)을 준수하도록 설계되었습니다. VVSG 2.0에 의해 주도된 가장 중요한 구현 선택 사항은 다음과 같습니다.

**투표 기록 불변성**(VVSG 2.0 Vol. I §3.3에 매핑됨)은 `VotingResult`를 리포지토리에 설정자(setter)와 업데이트 작업이 없는 완전히 불변인 JPA 엔티티로 만들어 구현됩니다. 관리자의 수정 작업은 별도의 `VoteCorrection` 엔티티와 이중 승인 BPMN 프로세스를 거치며, 원본 기록은 손대지 않은 상태로 유지됩니다.

**감사 추적 완전성**(VVSG 2.0 Vol. I §12에 매핑됨)은 `@AuditAction` 어노테이션과 `AuditEventPublisher`를 통해 해결되며, 선거 데이터의 상태를 변경하는 모든 작업이 추가 전용(append-only) 테이블에 `AuditEvent` 기록을 생성하고 Kafka 감사 토픽에 발행되도록 보장합니다. 이 감사 로그는 전용 제한 API를 통해 감사자가 조회할 수 있습니다.

**유권자 개인정보 및 PII 보호**는 JPA 컨버터 수준에서 데이터베이스에 기록되기 전에 `VoterRecord.externalVoterId`를 AES-256으로 암호화하여 처리됩니다. 응답 DTO는 절대로 원시 유권자 ID를 노출하지 않습니다.

**접근성**(VVSG 2.0 Vol. I §4에 매핑됨)은 유권자 대상 UI에 대해 WCAG 2.1 Level AA 준수를 요구합니다. 투표 키오스크 페이지(`VotingSessionPage`)는 큰 클릭 영역, 고대비 테마, 완전한 키보드 탐색 및 ARIA 역할을 기반으로 구축되며, 테스트 스위트에서 axe-core를 사용하여 WCAG 기준에 대해 검증됩니다.

모든 VVSG 2.0 요구사항을 MiREMS 모듈이나 기능에 매핑하는 전체 추적 매트릭스는 `docs/vvsg/VVSG2_MAPPING.md`에 유지 관리됩니다.

---

## 9. 시작하기

### 전제 조건

시작하기 전에 다음이 설치되어 있는지 확인하십시오.

* 로컬 인프라 실행을 위한 Docker Desktop (또는 동급 소프트웨어)
* Amazon Corretto JDK 21
* Gradle 8.10.2 (또는 Gradle 래퍼 사용: `./gradlew`)
* Node.js 22 LTS
* pnpm 9 (`npm install -g pnpm`)
* Make (Makefile 편의 타겟용)

### 로컬 개발 환경 설정

리포지토리를 클론하고 환경 변수 템플릿을 복사합니다.

```bash
git clone [https://github.com/mirems/mirems-platform.git](https://github.com/mirems/mirems-platform.git)
cd mirems-platform
cp infrastructure/docker/.env.example infrastructure/docker/.env
# .env를 편집하여 MIREMS_ENCRYPTION_KEY 및 기타 비밀 키를 입력하세요

```

인프라 서비스(PostgreSQL, Keycloak, Kafka)를 시작합니다.

```bash
make dev-up

```

백엔드를 빌드하고 실행합니다. 시작 시 Flyway 마이그레이션이 자동으로 실행됩니다.

```bash
./gradlew :backend:mirems-core:core-api:bootRun

```

새 터미널에서 프론트엔드 의존성을 설치하고 개발 서버를 시작합니다.

```bash
cd frontend
pnpm install
pnpm -r build          # 공유 패키지를 먼저 빌드합니다
cd mirems-shell
pnpm dev               # http://localhost:5173 에서 Vite 개발 서버 실행

```

### 테스트 실행

백엔드 테스트는 Testcontainers를 사용하며 Docker가 실행 중이어야 합니다.

```bash
./gradlew test                    # 모든 백엔드 테스트
./gradlew jacocoTestReport        # 커버리지 보고서 생성
cd frontend && pnpm -r test       # 모든 프론트엔드 테스트

```

### 확장팩 활성화

한국 선거 확장팩을 활성화하려면 `application.yml` 또는 환경 변수에 다음을 추가하세요:

```yaml
mirems:
  extension:
    kr:
      enabled: true
    us:
      enabled: false

```

---

## 10. AI 에이전트 개발 가이드

MiREMS는 AI 코딩 에이전트(Hermes)가 여러 세션에 걸쳐 점진적으로 구축하도록 설계되었습니다. 다음 원칙들이 이를 지속 가능하게 만듭니다.

**한 번에 하나의 목표.** `PLAN.md`의 각 `GOAL`은 30-60분의 단일 에이전트 세션에서 완료할 수 있도록 설계되었습니다. 목표에는 명시적인 작업 목록, 완료 기준(done criteria) 및 종속성 선언이 있습니다. 에이전트는 모든 종속성이 `DONE`으로 확인되기 전에는 절대 목표를 시작해서는 안 됩니다.

**컨텍스트 우선 규율.** 코드를 작성하기 전에 에이전트는 목표에 나열된 `CONTEXT files`를 읽습니다. 이는 에이전트가 이미 기록된 ADR이나 VVSG 매핑과 모순되는 아키텍처 결정을 내리는 것을 방지합니다.

**테스트 게이트는 타협할 수 없습니다.** 테스트를 통과하고 커버리지가 임계값을 충족할 때까지 목표는 `DONE`이 아닙니다. CI 파이프라인이 이를 강제하지만, 에이전트는 목표를 완료로 표시하기 전에 자체적으로 검증해야 합니다.

**커밋 메시지 형식**은 추적성을 인코딩합니다: `[GOAL-P1-009] feat: Add Election aggregate with state machine`. 이를 통해 전체 개발 내역을 특정 계획 항목으로 추적할 수 있습니다.

특정 목표에 대한 에이전트 세션을 시작하려면 목표 ID를 대체하고 컨텍스트 파일을 나열하여 `AGENTS.md §10`의 프롬프트 템플릿을 사용하세요. 그러면 에이전트는 전체적인 그림을 위해 이 README를 읽고, 제약 조건을 위해 `AGENTS.md`를 읽은 다음, `PLAN.md`의 작업을 실행합니다.

---

## 11. 기여하기

모든 코드 기여는 `PLAN.md`의 목표 시스템을 거칩니다. 인간 기여자는 작업을 시작하기 전에 목표를 `IN_PROGRESS`로 표시하고 목표 ID를 참조하는 풀 리퀘스트(PR)를 제출해야 합니다. PR 설명에는 모든 완료 기준이 충족되었으며 CI 파이프라인을 통과했음을 확인해야 합니다.

VVSG 2.0 규정 준수 상태에 영향을 미치는 변경의 경우, 동일한 PR에서 `docs/vvsg/VVSG2_MAPPING.md`를 업데이트해야 합니다. 새로운 ADR의 경우 `docs/adr/ADR-000-template.md`의 형식을 따르세요.

확장팩 기여는 `AGENTS.md §9`에 정의된 확장팩 개발 계약(Extension Pack Development Contract)을 준수해야 합니다.

---

*MiREMS Platform — 한 번에 하나의 프로세스씩, 신뢰할 수 있는 선거를 구축합니다.*

```
