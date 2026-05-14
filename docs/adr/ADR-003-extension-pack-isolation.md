# ADR-003: Extension Pack Isolation

**Status:** Accepted

**Date:** 2026-05-14

## Context

MiREMS is intended to support multiple countries and jurisdictions. Election law, ballot formats, eligibility rules, calendars, and tabulation methods differ by country. The core platform must remain country-agnostic so that adding a new country does not destabilize existing deployments.

At the same time, country-specific behavior must integrate into the runtime through clear extension points for domain rules, BPMN/DMN assets, database migrations, and UI route/components.

## Decision

Use isolated Extension Packs.

- Core modules define stable extension interfaces and registries.
- Extension modules depend on Core; Core must never depend on `ext-*` modules.
- Backend extension packs are Spring Boot auto-configuration modules activated by properties such as `mirems.extension.kr.enabled=true`.
- Country-specific DMN/BPMN assets live under extension resource paths such as `processes/ext/{code}/`.
- Country-specific Flyway migrations live under extension-specific migration paths.
- Frontend extension packages export route segments and components that the shell registers when enabled.
- Build rules and tests should enforce that Core does not import extension packages.

## Consequences

Positive consequences:

- Country-specific law and workflows are isolated from the universal election domain model.
- New jurisdictions can be added without modifying Core internals.
- Deployments can enable only the extension packs they need.
- Legal traceability can be maintained per extension under `docs/extensions/{code}/LEGAL.md`.

Negative consequences:

- Extension points must be designed carefully before heavy country-specific work begins.
- Runtime discovery and conditional loading add complexity.
- Shared logic between extensions must be extracted to `ext-common` without leaking country-specific behavior into Core.
- Database migration ordering must avoid collisions across extension packs.

## Compliance Notes

Extension packs must document their legal/regulatory basis. Any country-specific rule affecting eligibility, ballot presentation, tabulation, audit, or certification must be traceable to legal references and to VVSG mapping where applicable.
