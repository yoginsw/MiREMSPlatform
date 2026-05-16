# US Election Law Assumptions for MiREMS ext-us

> 상태: 구현용 간소화 스냅샷입니다. 실제 주별 선거법 적용 전 법무 검토가 필요합니다.

## HAVA voter eligibility / provisional ballot assumptions

- Federal voter eligibility requires United States citizenship.
- A voter must be registered before a regular ballot can be issued.
- For general elections, the voter must be at least 18 years old on election day.
- HAVA first-time mail registration ID verification failure does not make the voter ineligible in this model; it requires a provisional ballot.
- Reason strings must remain generic and must not include PII.

## State-specific primary age assumptions

- Default rule: primary voters must be 18 on the primary election day.
- Snapshot exception: Maryland (`MD`) permits a primary voter who will be 18 by the following general election day.
- Additional state variations require legal review before production use.

## UOCAVA absentee ballot assumptions

- Active-duty military voters and overseas citizen voters are UOCAVA-eligible.
- If a UOCAVA voter has not received a blank ballot and the request is within 45 days before election day, the model allows the Federal Write-In Absentee Ballot fallback.
- Domestic no-excuse absentee eligibility is represented as a simplified state-law snapshot, not a complete 50-state legal matrix.
