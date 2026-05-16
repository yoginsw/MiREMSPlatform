# VVSG 2.0 E2E Compliance Evidence Checklist

This checklist defines the evidence package for P9-089—P9-096. Each run should capture Playwright traces/screenshots, k6 summaries, server logs, audit exports, and reviewer sign-off.

## Election lifecycle

- Verify election listing, detail review, publish, close, and status transitions with role-scoped access.
- Capture API request/response evidence for election state changes.
- Confirm state transitions preserve audit records and do not bypass eligibility or ballot controls.

## Voting session

- Verify voter session creation, ballot preview, selection, review, cast receipt, and spoil flow.
- Confirm keyboard operation, high contrast, large text, and clear error messaging.
- Preserve receipt evidence without exposing voter selections beyond authorized workflow boundaries.

## Tabulation and certification

- Verify tabulation workflow start, process monitoring, result display, and certification evidence.
- Confirm official result export is reproducible and tied to election ID and generated timestamp.
- Record any reconciliation discrepancies and resolution steps.

## Audit trail and chain of custody

- Verify append-only audit event search, CSV export, event timestamps, actor IDs, source IP, and aggregate IDs.
- Confirm chain-of-custody evidence covers election lifecycle, voting session, tabulation, and certification.
- Include reviewer notes for missing or unexpected event types.

## Accessibility and usability

- Run critical paths with Playwright accessibility assertions and manual screen-reader spot checks where available.
- Confirm Korean and English user-facing text avoids mid-word Korean line breaks and remains readable at large text settings.
- Capture screenshots for normal, high-contrast, and large-text modes.

## Security and privacy

- Verify protected routes fail closed without authentication and enforce role/election scope checks.
- Confirm load and E2E evidence does not include passwords, OIDC tokens, raw PII, or secret values.
- Record dependency, lint, and build verification outputs.

## Load and resilience

- Execute `k6 run frontend/e2e/load/k6-voting-load.js` against a non-production environment configured for 10k concurrent voter simulation.
- Execute `MIREMS_CHAOS_ENABLED=true k6 run frontend/e2e/chaos/k6-api-chaos.js` against a resilience test environment.
- Archive k6 JSON summaries, service metrics, database metrics, and incident notes.
