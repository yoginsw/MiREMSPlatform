package io.mirems.core.domain.result;

/** Lifecycle state for vote correction records. */
public enum CorrectionStatus {
    PENDING_APPROVAL,
    FIRST_APPROVED,
    APPROVED,
    REJECTED
}
