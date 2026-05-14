# ADR-004: Immutable Vote Records and Correction Workflow

**Status:** Accepted

**Date:** 2026-05-14

## Context

Vote records are among the most sensitive records in an election system. Once a vote is cast and committed, modifying that record in place would compromise auditability, public trust, and the ability to reconstruct the original election state.

Administrative correction scenarios can exist, but they must not overwrite original vote records. Corrections require explicit approval, audit evidence, and traceability from the corrected record back to the original immutable record.

## Decision

Make `VotingResult` append-only and immutable after persistence.

- `VotingResult` entities have no setters for persisted fields.
- Repositories expose save and read methods only; no update or delete operations are allowed for vote records.
- Database schema must prevent or detect in-place modification where practical.
- Each vote record stores a deterministic SHA-256 hash derived from its committed fields.
- Administrative corrections are represented by separate `VoteCorrection` records.
- Vote correction must be performed through `VoteCorrectionProcess.bpmn` with dual approval by separate authorized users.
- The original `VotingResult` is never modified by a correction.

## Consequences

Positive consequences:

- Original vote records remain preserved for audit and recount analysis.
- Corrections become explicit, reviewable, and independently auditable.
- Hashing supports tamper-evidence and integrity verification.
- The design aligns with election integrity expectations and VVSG auditability goals.

Negative consequences:

- Query logic must account for original records plus correction records.
- Storage grows append-only over time.
- Correction workflows are more complex than simple updates.
- Developers must avoid generic repository patterns that expose delete or update operations for vote records.

## Compliance Notes

This decision directly supports vote data integrity, audit trail completeness, and post-election verification. `docs/vvsg/VVSG2_MAPPING.md` must trace immutable vote record handling and correction workflow requirements to the applicable VVSG 2.0 sections.
