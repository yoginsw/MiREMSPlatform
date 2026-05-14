package io.mirems.core.api.web;

import io.mirems.core.domain.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ObservabilityProbeController {
    private static final Logger log = LoggerFactory.getLogger(ObservabilityProbeController.class);

    @GetMapping("/api/observability/probe")
    ProbeResponse probe(
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "unscoped") String electionId) {
        try (MDC.MDCCloseable ignoredUser = MDC.putCloseable("userId", userId);
                MDC.MDCCloseable ignoredElection = MDC.putCloseable("electionId", electionId)) {
            log.info("MiREMS observability probe executed");
            return new ProbeResponse("ok");
        }
    }

    @GetMapping("/api/observability/probe/validation-error")
    ProbeResponse validationError() {
        throw new ValidationException("MIR-VAL-PROBE", "Probe validation error");
    }

    record ProbeResponse(String status) {
    }
}
