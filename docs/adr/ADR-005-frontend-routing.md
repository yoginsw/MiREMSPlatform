# ADR-005: TanStack Router File-Based Routing

**Status:** Accepted

**Date:** 2026-05-14

## Context

The MiREMS frontend must support a React shell, authenticated administrative pages, future voter-facing flows, audit pages, and dynamically registered extension-pack routes. Route definitions must be type-safe and maintainable as the application grows.

The frontend stack selected in README is React 19, TypeScript 5.9, Vite 6, and TanStack Router.

## Decision

Use TanStack Router with file-based routing for the React shell.

- The main shell routes live under `frontend/mirems-shell/src/routes/`.
- Public routes are explicitly separated from protected routes.
- Protected routes must check OIDC authentication and role/scope authorization before rendering sensitive screens.
- Extension UI packages export route segments/components that the shell can register when the extension is enabled.
- Route params, loaders, and navigation should use TanStack Router type-safety.
- TanStack Router Devtools may be enabled only in development builds.

## Consequences

Positive consequences:

- Route structure is visible in the file tree.
- Type-safe params and navigation reduce runtime routing errors.
- Extension route registration can be standardized.
- Protected route behavior can be tested at route boundaries.

Negative consequences:

- The team must follow TanStack Router conventions rather than ad-hoc React Router patterns.
- Generated route artifacts, if introduced, must be handled consistently in builds.
- Extension route loading needs a clear plugin contract to avoid shell/extension coupling.

## Compliance Notes

Voter-facing routes must satisfy accessibility requirements and must not expose direct database access. Administrative routes must enforce role and election-scope restrictions in the UI, while the backend remains the authoritative enforcement point.
