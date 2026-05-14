# MiREMS Role-Permission Matrix

**Status:** Initial RBAC baseline  
**Source ADR:** `docs/adr/ADR-001-auth-strategy.md`  
**Realm:** `mirems`  
**OIDC Provider:** Keycloak 24.x  
**Last updated:** 2026-05-14

> 이 문서는 MiREMS의 권한 설계 기준입니다. 실제 API 엔드포인트가 구현되면 각 컨트롤러/Operation ID 단위로 더 세분화해야 합니다. 현재 단계에서는 Phase 0 기준의 초기 역할-권한 기준선입니다.

---

## 1. Authentication Model

- Frontend: OIDC Authorization Code Flow with PKCE
- Backend: OAuth2 Resource Server validating Keycloak JWT access tokens
- Realm: `mirems`
- Public frontend client: `mirems-frontend`
- Confidential backend/service client: `mirems-backend`
- Role source: Keycloak realm roles
- Spring Security authority mapping target: `ROLE_{ROLE_NAME}`

---

## 2. Realm Roles

| Role | Purpose | Notes |
|---|---|---|
| `SYSTEM_ADMIN` | 플랫폼 전체 관리 | 모든 운영 역할을 포함하는 최상위 composite role |
| `ELECTION_ADMIN` | 선거 설정/공표/운영자 지정 | 선거 운영과 감사 조회 권한을 포함 |
| `ELECTION_OFFICER` | 후보자, 투표지, 선거인명부, 투표 운영 | 선거 실무 담당자 |
| `TABULATION_OFFICER` | 개표, 집계, 결과 검증, 재검표 지원 | 결과 데이터 변경은 감사 이벤트 필수 |
| `AUDITOR` | 감사 이력, 증적, 정책 준수 검토 | 원칙적으로 read-only + 감사 워크플로 참여 |
| `OBSERVER` | 공개/허가된 선거 상태와 결과 조회 | read-only |
| `VOTER` | 향후 voter portal용 | 관리자 기능 접근 금지 |

---

## 3. Composite Role Hierarchy

```text
SYSTEM_ADMIN
├── ELECTION_ADMIN
│   ├── ELECTION_OFFICER
│   │   └── OBSERVER
│   ├── TABULATION_OFFICER
│   │   └── OBSERVER
│   ├── AUDITOR
│   │   └── OBSERVER
│   └── OBSERVER
└── VOTER
```

Design notes:

- `SYSTEM_ADMIN`은 플랫폼 break-glass 및 초기 운영을 위해 모든 realm role을 포함합니다.
- `ELECTION_ADMIN`은 선거 설정과 운영 승인 책임자이므로 실무/집계/감사 조회 역할을 포함합니다.
- `OBSERVER`는 read-only 기반 역할이며 여러 운영 역할의 최소 조회 권한으로 재사용합니다.
- `VOTER`는 향후 voter-facing flow를 위한 별도 역할입니다. 운영자 composite에는 포함하지 않습니다. 단, `SYSTEM_ADMIN`에는 realm 전체 테스트/관리 편의를 위해 포함했습니다.

---

## 4. Permission Matrix by Capability

| Capability | SYSTEM_ADMIN | ELECTION_ADMIN | ELECTION_OFFICER | TABULATION_OFFICER | AUDITOR | OBSERVER | VOTER |
|---|---:|---:|---:|---:|---:|---:|---:|
| Platform configuration | Allow | Deny | Deny | Deny | Deny | Deny | Deny |
| Identity/role administration | Allow | Deny | Deny | Deny | Deny | Deny | Deny |
| Election create/update | Allow | Allow | Limited | Deny | Deny | Deny | Deny |
| Election publish workflow initiate | Allow | Allow | Deny | Deny | Deny | Deny | Deny |
| Candidate registration workflow | Allow | Allow | Allow | Deny | Read | Read | Deny |
| Ballot configuration | Allow | Allow | Allow | Deny | Read | Read | Deny |
| Voter roll import/update | Allow | Allow | Allow | Deny | Read masked | Deny | Deny |
| Voting session operation | Allow | Allow | Allow | Deny | Read audit only | Deny | Future self-service only |
| Tabulation execution | Allow | Allow | Deny | Allow | Read | Read published only | Deny |
| Result certification workflow | Allow | Allow | Deny | Allow participate | Read | Read published only | Deny |
| Vote correction workflow | Allow | Allow approve | Initiate where scoped | Deny | Review | Deny | Deny |
| Audit log query | Allow | Allow scoped | Limited scoped | Limited scoped | Allow scoped | Deny | Deny |
| Published result view | Allow | Allow | Allow | Allow | Allow | Allow | Future public/self-service |
| Extension pack configuration | Allow | Allow scoped | Deny | Deny | Read | Read | Deny |

Legend:

- `Allow`: capability is permitted when election-scope checks pass.
- `Limited`: only specific operational actions are permitted; policy must be enforced in service/API code.
- `Read`: read-only access, normally with masking where PII is involved.
- `Read masked`: PII must be masked or encrypted values must not be exposed.
- `Deny`: access must be rejected and logged as a security audit event where applicable.

---

## 5. Initial API Authorization Policy

Future REST controllers should apply these baseline rules.

| API Area | Required Role Baseline | Additional Rule |
|---|---|---|
| `/api/admin/**` | `SYSTEM_ADMIN` | Security audit on all denied attempts |
| `/api/elections` write operations | `ELECTION_ADMIN` | Must pass election-scope check |
| `/api/elections` read operations | `OBSERVER` or stronger | Return only scoped elections |
| `/api/candidates/**` write operations | `ELECTION_OFFICER` or `ELECTION_ADMIN` | Candidate workflow actions must emit audit events |
| `/api/ballots/**` write operations | `ELECTION_OFFICER` or `ELECTION_ADMIN` | Ballot changes require audit events |
| `/api/voter-rolls/**` | `ELECTION_OFFICER` or `ELECTION_ADMIN` | PII masking/encryption required |
| `/api/tabulation/**` | `TABULATION_OFFICER` or `ELECTION_ADMIN` | Results must remain immutable after commit |
| `/api/audit/**` | `AUDITOR`, `ELECTION_ADMIN`, or `SYSTEM_ADMIN` | Append-only evidence; no update/delete API |
| `/api/results/published/**` | `OBSERVER` or public policy | Jurisdiction-dependent publication policy |
| `/api/voter/**` | `VOTER` | Future voter portal; must not expose operator APIs |

---

## 6. Election-Scoped Authorization

Realm roles are necessary but not sufficient. MiREMS must also enforce election scope.

Required checks:

1. JWT contains an operator identity.
2. Requested `electionId` is within the operator's assigned scope.
3. Role grants the requested capability.
4. Denied access produces a security audit event.
5. Any successful state-changing operation emits a domain/audit event.

Scope representation is not finalized in P0. Until the domain model is implemented, scope may be represented by test fixtures or future claims such as:

```json
{
  "mirems_election_scope": ["election-uuid-1", "election-uuid-2"]
}
```

This claim shape is provisional and must be revisited during the API/security implementation goals.

---

## 7. VVSG / Audit Notes

- Role violations and election-scope violations are security-relevant events and must be logged.
- Access to voter PII must be minimized, masked, or encrypted according to the data protection design.
- Vote records and tabulation outputs must not be mutable through ordinary role permissions.
- Audit APIs must not expose update/delete operations.
- `OBSERVER` must not receive privileged draft, voter, or internal audit data unless explicitly authorized by jurisdiction policy.
