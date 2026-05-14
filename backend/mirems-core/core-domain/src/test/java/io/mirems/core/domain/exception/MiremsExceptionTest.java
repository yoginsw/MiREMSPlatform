package io.mirems.core.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class MiremsExceptionTest {
    @Test
    void validationExceptionCarriesStableErrorMetadata() {
        ValidationException exception = new ValidationException("MIR-VAL-001", "Election name is required");

        assertEquals("MIR-VAL-001", exception.getErrorCode());
        assertEquals("Validation failed", exception.getTitle());
        assertEquals("Election name is required", exception.getMessage());
    }

    @Test
    void domainExceptionPreservesCause() {
        IllegalStateException cause = new IllegalStateException("bad transition");

        DomainException exception = new DomainException("MIR-DOM-001", "Invalid state transition", cause);

        assertSame(cause, exception.getCause());
        assertEquals("Domain rule violation", exception.getTitle());
    }
}
