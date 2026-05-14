# ADR-001: Keycloak + OIDC/PKCE Authentication Strategy

**Status:** Accepted

**Date:** 2026-05-14

## Context

MiREMS Platform must support multiple operator roles across election administration, tabulation, audit, observation, and future voter-facing flows. Authentication must be standards-based, externally auditable, and compatible with browser-based clients and backend APIs. The frontend is a React application, the backend is Spring Boot, and deployment targets may vary by country or jurisdiction.

The platform also has strict requirements around role-based access control, election-scoped authorization, security-event audit logging, and avoiding direct credential handling in application code.

## Decision

Use Keycloak as the OpenID Connect identity provider.

- The browser frontend uses OIDC Authorization Code Flow with PKCE.
- The backend acts as an OAuth2 Resource Server and validates JWT access tokens issued by Keycloak.
- The frontend client is public and uses PKCE.
- Backend service clients are confidential where service-to-service access is required.
- Roles are represented as Keycloak realm/client roles and mapped to Spring Security authorities.
- The canonical role-permission matrix is maintained in `docs/auth/ROLE_MATRIX.md`.
- Application code must not hardcode credentials or secrets; all Keycloak URLs, realms, clients, and secrets come from environment variables.

## Consequences

Positive consequences:

- Authentication is delegated to a mature OIDC provider rather than custom login code.
- Browser login can avoid storing long-lived credentials in the application.
- Backend authorization can use standard Spring Security JWT validation.
- Role and user lifecycle management can be handled operationally through Keycloak.
- The approach supports future federation with external identity providers.

Negative consequences:

- Local development and CI require a Keycloak realm configuration.
- Authorization tests must include representative JWT claims for every role.
- Keycloak version upgrades become part of the platform compatibility matrix.
- Realm export/import must be controlled to avoid configuration drift.

## Compliance Notes

Authentication failures, role violations, and election-scope violations must be recorded as security audit events. Any endpoint that touches election, ballot, voting result, or audit data must enforce role and scope checks before executing business logic.
