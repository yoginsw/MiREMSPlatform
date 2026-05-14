# MiREMS Platform PoC 테스터용 BPMN/DMN 변경 테스트 시나리오

## 1. 문서 목적

이 문서는 MiREMS Platform PoC를 처음 접하는 테스터가 다음 사항을 단계별로 확인할 수 있도록 작성되었습니다.

- Admin 화면에서 BPMN과 DMN을 확인하는 방법
- BPMN XML 또는 BPMN Diagram을 수정하고 배포하는 방법
- DMN XML 또는 DMN Decision Table을 수정하고 배포하는 방법
- 변경 전/후 결과를 Admin 화면과 Swagger UI에서 비교하는 방법
- 어떤 변경이 화면 표시만 바꾸고, 어떤 변경이 API 판단 결과를 바꾸는지 이해하는 방법

> 중요: 이 PoC의 BPMN은 프로세스 실행 엔진으로 직접 실행되는 모델이 아닙니다. BPMN은 관리, 시각화, 요약, 버전 배포 확인 용도입니다. 실제 API 판단 결과는 활성 DMN Decision Table을 기반으로 계산됩니다.

---

## 2. PoC 핵심 개념

### 2.1 BPMN의 역할

BPMN은 voter registration 업무 흐름을 표현합니다.

예시 업무 흐름:

1. 유권자 등록 신청 수신
2. 신청서 완전성 확인
3. 보완 필요 여부 판단
4. 거주지 및 개인정보 검증
5. 등록 가능 여부 판단
6. 유권자 등록 또는 부적격 통지

이 PoC에서 BPMN 변경은 다음 항목으로 확인합니다.

- Admin 화면의 BPMN Diagram 표시 변경
- Admin 화면의 BPMN XML 변경
- 현재 활성 배포의 프로세스명 변경
- BPMN 요약 영역의 작업, 게이트웨이, 종료 이벤트 이름 변경
- 새 배포 버전 생성 여부

### 2.2 DMN의 역할

DMN은 voter registration 요청을 판단하는 의사결정 규칙입니다.

API 요청 입력값:

- `applicantName`: 신청자 이름
- `age`: 나이
- `registrationComplete`: 등록 신청서 완전성 여부
- `residenceVerified`: 거주지 검증 여부
- `clarificationRequired`: 추가 보완 필요 여부

API 응답의 핵심 판단 결과:

- `outcome`
- `nextStep`
- `auditReason`

이 PoC에서 실제 `/api/voter-registration/register` API 결과는 활성 DMN XML의 규칙에 따라 바뀝니다.

---

## 3. 사전 준비

## 3.1 애플리케이션 실행

프로젝트 루트에서 실행합니다.

```bash
cd /mnt/d/workspace/miremsPoC
docker compose up --build
```

정상 실행 후 브라우저에서 다음 URL을 엽니다.

- Admin 화면: `http://localhost:8080/mirems/admin`
- Swagger UI: `http://localhost:8080/mirems/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/mirems/v3/api-docs`

## 3.2 Admin 화면에서 확인할 영역

Admin 화면에서 다음 영역을 확인합니다.

- 현재 활성 배포
- BPMN 요약
- 새 프로세스 정의 배포
- BPMN 작업 영역
  - `BPMN Diagram` 탭
  - `BPMN XML` 탭
- DMN 작업 영역
  - `DMN Decision Table` 탭
  - `DMN XML` 탭
- 최근 배포 버전
- 의사결정 감사 이력

## 3.3 배포 버튼

BPMN 또는 DMN을 수정한 뒤에는 반드시 다음 버튼을 누릅니다.

- `배포 및 활성화`

이 버튼을 누르면 다음 일이 발생합니다.

1. BPMN Diagram 변경 사항이 BPMN XML로 자동 반영됩니다.
2. DMN Decision Table 변경 사항이 DMN XML로 자동 반영됩니다.
3. 서버에 새 프로세스 정의 버전이 저장됩니다.
4. 기존 활성 버전은 비활성화됩니다.
5. 새 버전이 활성 버전이 됩니다.
6. 이후 API 호출은 새 활성 DMN을 사용합니다.

---

## 4. 기본 상태 확인 시나리오

## 4.1 Admin 화면 기본 상태 확인

1. `http://localhost:8080/mirems/admin` 접속
2. `현재 활성 배포` 영역 확인
3. 다음 값을 기록합니다.

예상 예시:

- 버전: `1` 또는 현재 활성 버전 ID
- 프로세스: `Register Voter`
- 배포자: `system-bootstrap`

4. `BPMN 요약` 영역 확인

예상 예시:

- 작업에 `Check for completeness`가 포함됨
- 작업에 `Create or update voter record`가 포함됨
- 게이트웨이에 `Registration complete?`가 포함됨
- 종료 이벤트에 `Voter Registered`가 포함됨

## 4.2 Swagger UI에서 기본 API 결과 확인

1. `http://localhost:8080/mirems/swagger-ui.html` 접속
2. `Voter Registration` 섹션을 펼칩니다.
3. `POST /api/voter-registration/register`를 선택합니다.
4. `Try it out` 버튼을 누릅니다.
5. Request body에 다음 JSON을 입력합니다.

```json
{
  "applicantName": "Alice Baseline",
  "age": 32,
  "registrationComplete": true,
  "residenceVerified": true,
  "clarificationRequired": false
}
```

6. `Execute`를 누릅니다.
7. Response body를 확인합니다.

예상 결과:

```json
{
  "definitionVersionId": 1,
  "auditId": 1,
  "applicantName": "Alice Baseline",
  "decision": {
    "outcome": "VOTER_REGISTERED",
    "nextStep": "Create or update voter record",
    "auditReason": "eligible adult with complete verified application"
  }
}
```

> `definitionVersionId`와 `auditId`는 환경에 따라 달라질 수 있습니다. 핵심 확인값은 `decision.outcome`, `decision.nextStep`, `decision.auditReason`입니다.

## 4.3 기본 DMN 판단표 이해

초기 DMN 규칙은 대략 다음 의미를 가집니다.

- 신청서가 미완성인 경우
  - outcome: `CLARIFICATION_REQUIRED`
  - nextStep: `Clarify with applicant`

- 추가 보완이 필요한 경우
  - outcome: `CLARIFICATION_REQUIRED`
  - nextStep: `Clarify with applicant`

- 거주지 검증이 실패한 경우
  - outcome: `INELIGIBLE`
  - nextStep: `Notify applicant of ineligibility`

- 18세 미만인 경우
  - outcome: `PENDING_AGE_ELIGIBILITY`
  - nextStep: `Turn 18 or Eligible to Vote`

- 18세 이상이고 신청서와 거주지가 모두 정상인 경우
  - outcome: `VOTER_REGISTERED`
  - nextStep: `Create or update voter record`

---

## 5. 테스트 시나리오 A: BPMN 프로세스명 변경 확인

## 5.1 목적

BPMN XML을 수정하고 배포하면 Admin 화면의 활성 프로세스명과 버전이 바뀌는지 확인합니다.

이 시나리오는 API 판단 결과를 바꾸는 테스트가 아닙니다. BPMN 관리 및 배포 버전 변경 확인용입니다.

## 5.2 변경 전 확인

Admin 화면에서 다음 값을 확인합니다.

- 현재 활성 배포 > 프로세스: `Register Voter`
- 최근 배포 버전의 최상단 버전 ID

## 5.3 BPMN XML 수정 샘플

1. Admin 화면에서 `BPMN 작업 영역`으로 이동합니다.
2. `BPMN XML` 탭을 선택합니다.
3. XML에서 다음 부분을 찾습니다.

```xml
<semantic:process isExecutable="false" name="Register Voter" id="_18_5_1_11940316_1498140978086_535172_20552"/>
```

4. `name` 값을 다음처럼 변경합니다.

```xml
<semantic:process isExecutable="false" name="Register Voter - QA Scenario A" id="_18_5_1_11940316_1498140978086_535172_20552"/>
```

수정 포인트:

- 변경 전: `name="Register Voter"`
- 변경 후: `name="Register Voter - QA Scenario A"`

## 5.4 배포

1. 화면 하단의 `배포 및 활성화` 버튼을 누릅니다.
2. 성공 메시지가 표시되는지 확인합니다.
3. 화면 상단 `현재 활성 배포` 영역으로 이동합니다.

## 5.5 변경 후 확인

예상 결과:

- 현재 활성 배포 > 프로세스: `Register Voter - QA Scenario A`
- 버전 ID가 이전보다 증가
- 최근 배포 버전 목록에 새 버전이 추가됨
- 새 버전에 `활성` 표시가 붙음

## 5.6 Swagger UI로 활성 정의 확인

1. Swagger UI에서 `GET /api/voter-registration/active-definition`을 선택합니다.
2. `Try it out`을 누릅니다.
3. `Execute`를 누릅니다.
4. Response body를 확인합니다.

예상 결과 예시:

```json
{
  "id": 2,
  "processName": "Register Voter - QA Scenario A",
  "deployedAt": "2026-05-13T..."
}
```

확인 포인트:

- `processName`이 `Register Voter - QA Scenario A`로 바뀌었는지 확인합니다.
- `id`가 새 배포 버전 ID인지 확인합니다.

---

## 6. 테스트 시나리오 B: BPMN 작업 이름 변경 확인

## 6.1 목적

BPMN의 작업 이름을 변경하면 Admin 화면의 BPMN 요약에 반영되는지 확인합니다.

## 6.2 BPMN XML 수정 샘플

1. Admin 화면에서 `BPMN 작업 영역` > `BPMN XML` 탭을 선택합니다.
2. 다음 작업을 찾습니다.

```xml
<semantic:task completionQuantity="1" isForCompensation="false" startQuantity="1" name="Create or update voter record" id="_18_5_1_11940316_1498143631579_442956_21054">
```

3. `name` 값을 다음처럼 변경합니다.

```xml
<semantic:task completionQuantity="1" isForCompensation="false" startQuantity="1" name="Create or update voter record - QA verified" id="_18_5_1_11940316_1498143631579_442956_21054">
```

수정 포인트:

- 변경 전: `Create or update voter record`
- 변경 후: `Create or update voter record - QA verified`

## 6.3 배포

1. `배포 및 활성화` 버튼을 누릅니다.
2. Admin 화면 상단의 `BPMN 요약` 영역으로 이동합니다.

## 6.4 변경 후 확인

예상 결과:

- BPMN 요약 > 작업 목록에 `Create or update voter record - QA verified`가 표시됨
- 최근 배포 버전 목록에 새 버전이 추가됨
- 현재 활성 배포의 버전 ID가 증가함

> 주의: 이 변경은 BPMN 표시/요약 확인용입니다. API의 `decision.nextStep`은 DMN output을 기준으로 하므로, BPMN 작업 이름 변경만으로 API 응답의 `nextStep`이 자동 변경되지는 않습니다.

---

## 7. 테스트 시나리오 C: DMN output 변경으로 API 결과 변경 확인

## 7.1 목적

DMN Decision Table의 output 값을 변경하면 Swagger UI API 응답이 즉시 바뀌는지 확인합니다.

이 시나리오는 가장 이해하기 쉬운 DMN 변경 테스트입니다.

## 7.2 변경 전 API 결과 확인

Swagger UI에서 다음 요청을 실행합니다.

```json
{
  "applicantName": "Alice Before DMN Output Change",
  "age": 32,
  "registrationComplete": true,
  "residenceVerified": true,
  "clarificationRequired": false
}
```

변경 전 예상 결과:

```json
{
  "decision": {
    "outcome": "VOTER_REGISTERED",
    "nextStep": "Create or update voter record",
    "auditReason": "eligible adult with complete verified application"
  }
}
```

## 7.3 DMN Decision Table에서 수정하는 방법

1. Admin 화면에서 `DMN 작업 영역`으로 이동합니다.
2. `DMN Decision Table` 탭을 선택합니다.
3. `adult-eligible` 규칙에 해당하는 행을 찾습니다.

찾을 조건:

- Age: `>= 18`
- Registration complete: `true`
- Residence verified: `true`
- Clarification required: `false`

4. 해당 행의 output 값을 다음처럼 수정합니다.

변경 전:

- Outcome: `VOTER_REGISTERED`
- Next step: `Create or update voter record`
- Audit reason: `eligible adult with complete verified application`

변경 후:

- Outcome: `VOTER_REGISTERED_FAST_TRACK`
- Next step: `Create priority voter record`
- Audit reason: `QA scenario C fast-track rule applied`

5. 필요 시 `표 내용을 XML에 반영` 버튼을 누릅니다.
6. `배포 및 활성화` 버튼을 누릅니다.

## 7.4 DMN XML에서 직접 수정하는 방법

Decision Table 편집이 어렵거나 XML로 직접 확인하려면 `DMN XML` 탭에서 다음 rule을 찾습니다.

```xml
<rule id="adult-eligible">
  <inputEntry><text>&gt;= 18</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>false</text></inputEntry>
  <outputEntry><text>VOTER_REGISTERED</text></outputEntry><outputEntry><text>Create or update voter record</text></outputEntry><outputEntry><text>eligible adult with complete verified application</text></outputEntry>
</rule>
```

다음처럼 변경합니다.

```xml
<rule id="adult-eligible">
  <inputEntry><text>&gt;= 18</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>false</text></inputEntry>
  <outputEntry><text>VOTER_REGISTERED_FAST_TRACK</text></outputEntry><outputEntry><text>Create priority voter record</text></outputEntry><outputEntry><text>QA scenario C fast-track rule applied</text></outputEntry>
</rule>
```

수정 후 `배포 및 활성화` 버튼을 누릅니다.

## 7.5 변경 후 Swagger UI 결과 확인

Swagger UI에서 같은 조건의 요청을 다시 실행합니다.

```json
{
  "applicantName": "Alice After DMN Output Change",
  "age": 32,
  "registrationComplete": true,
  "residenceVerified": true,
  "clarificationRequired": false
}
```

변경 후 예상 결과:

```json
{
  "decision": {
    "outcome": "VOTER_REGISTERED_FAST_TRACK",
    "nextStep": "Create priority voter record",
    "auditReason": "QA scenario C fast-track rule applied"
  }
}
```

확인 포인트:

- `definitionVersionId`가 변경 전보다 증가했는지 확인합니다.
- `decision.outcome`이 `VOTER_REGISTERED_FAST_TRACK`으로 바뀌었는지 확인합니다.
- `decision.nextStep`이 `Create priority voter record`로 바뀌었는지 확인합니다.
- Admin 화면의 `의사결정 감사 이력`에 새 API 호출 이력이 추가되었는지 확인합니다.

---

## 8. 테스트 시나리오 D: DMN age threshold 변경으로 판단 분기 변경 확인

## 8.1 목적

DMN input 조건을 변경하여 같은 요청이 변경 전에는 등록 승인되고, 변경 후에는 연령 대기 상태로 바뀌는지 확인합니다.

이 시나리오는 룰 조건 변경이 API 결과에 영향을 주는지 확인하는 테스트입니다.

## 8.2 변경 전 API 결과 확인

Swagger UI에서 다음 요청을 실행합니다.

```json
{
  "applicantName": "Casey Age 20 Before",
  "age": 20,
  "registrationComplete": true,
  "residenceVerified": true,
  "clarificationRequired": false
}
```

초기 DMN 기준 예상 결과:

```json
{
  "decision": {
    "outcome": "VOTER_REGISTERED",
    "nextStep": "Create or update voter record",
    "auditReason": "eligible adult with complete verified application"
  }
}
```

> 만약 시나리오 C를 먼저 수행했다면 outcome/nextStep은 시나리오 C에서 바꾼 값으로 나올 수 있습니다. 이 시나리오 D의 핵심은 `age=20`이 변경 후 `PENDING_AGE_ELIGIBILITY`로 바뀌는지 확인하는 것입니다.

## 8.3 DMN XML 수정 샘플

Admin 화면에서 `DMN 작업 영역` > `DMN XML` 탭을 선택합니다.

### 8.3.1 minor rule 변경

다음 rule을 찾습니다.

```xml
<rule id="minor">
  <inputEntry><text>&lt; 18</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>false</text></inputEntry>
  <outputEntry><text>PENDING_AGE_ELIGIBILITY</text></outputEntry><outputEntry><text>Turn 18 or Eligible to Vote</text></outputEntry><outputEntry><text>applicant is under age threshold</text></outputEntry>
</rule>
```

다음처럼 변경합니다.

```xml
<rule id="minor">
  <inputEntry><text>&lt; 21</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>false</text></inputEntry>
  <outputEntry><text>PENDING_AGE_ELIGIBILITY</text></outputEntry><outputEntry><text>Wait until age 21 eligibility</text></outputEntry><outputEntry><text>QA scenario D age threshold changed to 21</text></outputEntry>
</rule>
```

### 8.3.2 adult-eligible rule 변경

다음 rule을 찾습니다.

```xml
<rule id="adult-eligible">
  <inputEntry><text>&gt;= 18</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>false</text></inputEntry>
  <outputEntry><text>VOTER_REGISTERED</text></outputEntry><outputEntry><text>Create or update voter record</text></outputEntry><outputEntry><text>eligible adult with complete verified application</text></outputEntry>
</rule>
```

다음처럼 age 조건만 `>= 21`로 변경합니다.

```xml
<rule id="adult-eligible">
  <inputEntry><text>&gt;= 21</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>true</text></inputEntry><inputEntry><text>false</text></inputEntry>
  <outputEntry><text>VOTER_REGISTERED</text></outputEntry><outputEntry><text>Create or update voter record</text></outputEntry><outputEntry><text>eligible adult with complete verified application</text></outputEntry>
</rule>
```

> 시나리오 C를 먼저 수행하여 `adult-eligible` output이 이미 바뀌어 있다면 output은 그대로 두고 age 조건만 `>= 21`로 바꾸어도 됩니다.

## 8.4 배포

1. `배포 및 활성화` 버튼을 누릅니다.
2. 성공 메시지를 확인합니다.
3. `최근 배포 버전`에 새 버전이 추가되었는지 확인합니다.

## 8.5 변경 후 Swagger UI 결과 확인

Swagger UI에서 같은 요청을 다시 실행합니다.

```json
{
  "applicantName": "Casey Age 20 After",
  "age": 20,
  "registrationComplete": true,
  "residenceVerified": true,
  "clarificationRequired": false
}
```

변경 후 예상 결과:

```json
{
  "decision": {
    "outcome": "PENDING_AGE_ELIGIBILITY",
    "nextStep": "Wait until age 21 eligibility",
    "auditReason": "QA scenario D age threshold changed to 21"
  }
}
```

## 8.6 추가 확인 요청

age가 21 이상이면 등록 승인 규칙을 타는지 확인합니다.

```json
{
  "applicantName": "Dana Age 21 After",
  "age": 21,
  "registrationComplete": true,
  "residenceVerified": true,
  "clarificationRequired": false
}
```

예상 결과:

```json
{
  "decision": {
    "outcome": "VOTER_REGISTERED",
    "nextStep": "Create or update voter record",
    "auditReason": "eligible adult with complete verified application"
  }
}
```

시나리오 C를 먼저 수행했다면 예상 결과는 다음처럼 나올 수 있습니다.

```json
{
  "decision": {
    "outcome": "VOTER_REGISTERED_FAST_TRACK",
    "nextStep": "Create priority voter record",
    "auditReason": "QA scenario C fast-track rule applied"
  }
}
```

---

## 9. 테스트 시나리오 E: 보완 필요 신청 건 확인

## 9.1 목적

신청서가 미완성인 경우 DMN이 보완 요청으로 판단하는지 확인합니다.

## 9.2 Swagger UI 요청

```json
{
  "applicantName": "Evan Incomplete",
  "age": 35,
  "registrationComplete": false,
  "residenceVerified": true,
  "clarificationRequired": false
}
```

예상 결과:

```json
{
  "decision": {
    "outcome": "CLARIFICATION_REQUIRED",
    "nextStep": "Clarify with applicant",
    "auditReason": "registration package is incomplete"
  }
}
```

확인 포인트:

- `registrationComplete=false`이므로 나이와 거주지 검증이 정상이어도 보완 요청이 우선 적용됩니다.
- DMN hit policy가 `FIRST`이므로 위쪽에서 먼저 매칭되는 rule이 결과가 됩니다.

---

## 10. 테스트 시나리오 F: 거주지 검증 실패 확인

## 10.1 목적

거주지 검증 실패 시 부적격 결과가 반환되는지 확인합니다.

## 10.2 Swagger UI 요청

```json
{
  "applicantName": "Riley Residence Failed",
  "age": 45,
  "registrationComplete": true,
  "residenceVerified": false,
  "clarificationRequired": false
}
```

예상 결과:

```json
{
  "decision": {
    "outcome": "INELIGIBLE",
    "nextStep": "Notify applicant of ineligibility",
    "auditReason": "residence verification failed"
  }
}
```

---

## 11. 결과 확인 위치 요약

## 11.1 Admin 화면

Admin 화면에서 확인할 항목:

- 현재 활성 배포
  - 버전 증가 여부
  - 프로세스명 변경 여부
  - 배포자 변경 여부
- BPMN 요약
  - 작업명 변경 여부
  - 게이트웨이명 변경 여부
  - 종료 이벤트명 변경 여부
- 최근 배포 버전
  - 새 버전 추가 여부
  - 활성 표시 여부
- 의사결정 감사 이력
  - Swagger UI로 호출한 신청자 이름 표시 여부
  - outcome 표시 여부
  - 호출 시각 기록 여부

## 11.2 Swagger UI

Swagger UI에서 확인할 항목:

- `GET /api/voter-registration/active-definition`
  - 활성 배포 ID
  - 활성 프로세스명
  - 배포 시각

- `POST /api/voter-registration/register`
  - `definitionVersionId`
  - `auditId`
  - `applicantName`
  - `decision.outcome`
  - `decision.nextStep`
  - `decision.auditReason`

---

## 12. Swagger UI 상세 사용 절차

## 12.1 활성 정의 확인

1. `http://localhost:8080/mirems/swagger-ui.html` 접속
2. `Voter Registration` 선택
3. `GET /api/voter-registration/active-definition` 선택
4. `Try it out` 클릭
5. `Execute` 클릭
6. Response body 확인

예상 Response body:

```json
{
  "id": 1,
  "processName": "Register Voter",
  "deployedAt": "2026-05-13T..."
}
```

## 12.2 유권자 등록 판단 API 호출

1. `POST /api/voter-registration/register` 선택
2. `Try it out` 클릭
3. Request body에 JSON 입력
4. `Execute` 클릭
5. Response body 확인

기본 정상 등록 요청:

```json
{
  "applicantName": "Swagger Tester",
  "age": 32,
  "registrationComplete": true,
  "residenceVerified": true,
  "clarificationRequired": false
}
```

기본 예상 결과:

```json
{
  "decision": {
    "outcome": "VOTER_REGISTERED",
    "nextStep": "Create or update voter record",
    "auditReason": "eligible adult with complete verified application"
  }
}
```

---

## 13. cURL로 동일 결과 확인하기

Swagger UI 대신 터미널에서 직접 호출할 수도 있습니다.

```bash
curl -X POST http://localhost:8080/mirems/api/voter-registration/register \
  -H "Content-Type: application/json" \
  -d '{"applicantName":"Curl Tester","age":32,"registrationComplete":true,"residenceVerified":true,"clarificationRequired":false}'
```

활성 정의 확인:

```bash
curl http://localhost:8080/mirems/api/voter-registration/active-definition
```

---

## 14. 테스트 체크리스트

| 구분 | 확인 항목 | 기대 결과 |
|---|---|---|
| 초기 상태 | Admin 접속 | 현재 활성 배포가 표시됨 |
| 초기 상태 | Swagger 정상 등록 요청 | `VOTER_REGISTERED` 반환 |
| BPMN 변경 | 프로세스명 변경 후 배포 | 활성 프로세스명이 변경됨 |
| BPMN 변경 | 작업명 변경 후 배포 | BPMN 요약 작업명이 변경됨 |
| DMN 변경 | adult-eligible output 변경 | API outcome/nextStep/auditReason 변경 |
| DMN 변경 | age threshold 18 → 21 변경 | age 20 요청이 `PENDING_AGE_ELIGIBILITY` 반환 |
| 감사 이력 | API 호출 후 Admin 확인 | 의사결정 감사 이력에 호출 기록 추가 |
| 버전 이력 | 배포 후 Admin 확인 | 최근 배포 버전에 새 버전 추가 및 활성 표시 |
| Swagger | active-definition 호출 | 최신 활성 버전 ID와 processName 반환 |

---

## 15. 자주 발생하는 실수와 확인 방법

## 15.1 수정했는데 결과가 바뀌지 않는 경우

확인할 사항:

1. `배포 및 활성화` 버튼을 눌렀는지 확인합니다.
2. DMN Decision Table 수정 후 `표 내용을 XML에 반영` 또는 `배포 및 활성화`를 수행했는지 확인합니다.
3. BPMN Diagram 수정 후 `다이어그램을 XML에 반영` 또는 `배포 및 활성화`를 수행했는지 확인합니다.
4. Swagger UI에서 이전 Request body를 그대로 다시 실행했는지 확인합니다.
5. Response의 `definitionVersionId`가 새 버전으로 바뀌었는지 확인합니다.

## 15.2 BPMN을 바꿨는데 API 결과가 바뀌지 않는 경우

정상 동작입니다.

이 PoC에서 API 판단 결과는 DMN Decision Table을 기준으로 합니다. BPMN은 업무 흐름의 관리, 시각화, 요약, 버전 배포를 확인하는 용도입니다.

## 15.3 DMN XML이 잘못되어 배포 또는 API 호출이 실패하는 경우

확인할 사항:

1. XML 태그가 정상적으로 닫혔는지 확인합니다.
2. `<rule>` 안에 inputEntry 4개와 outputEntry 3개가 유지되었는지 확인합니다.
3. age 조건은 지원 형식으로 작성했는지 확인합니다.

지원되는 age 조건 예시:

- `-`
- `< 18`
- `< 21`
- `<= 20`
- `>= 18`
- `>= 21`
- `32`

지원되는 boolean 조건 예시:

- `true`
- `false`
- `-`

## 15.4 DMN Decision Table 화면이 어색하게 보이는 경우

1. `DMN XML` 탭으로 이동했다가 다시 `DMN Decision Table` 탭으로 돌아옵니다.
2. 필요하면 브라우저 새로고침 후 다시 Admin 화면에 접속합니다.
3. `XML을 표로 다시 불러오기` 버튼을 누릅니다.

---

## 16. 권장 테스트 순서

처음 접하는 테스터에게는 다음 순서를 권장합니다.

1. Admin 화면 접속 및 기본 상태 확인
2. Swagger UI에서 기본 정상 등록 요청 실행
3. BPMN 프로세스명 변경 후 active-definition으로 확인
4. BPMN 작업명 변경 후 BPMN 요약에서 확인
5. DMN adult-eligible output 변경 후 API 응답 변경 확인
6. DMN age threshold 변경 후 age 20/21 요청 비교
7. Admin 화면에서 감사 이력과 배포 버전 확인

이 순서대로 진행하면 BPMN은 관리/시각화/버전 배포에 영향을 주고, DMN은 API 판단 결과에 영향을 준다는 차이를 명확하게 이해할 수 있습니다.
