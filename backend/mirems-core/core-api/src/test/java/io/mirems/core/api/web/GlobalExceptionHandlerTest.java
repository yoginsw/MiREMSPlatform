package io.mirems.core.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalExceptionHandlerTest {
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ObservabilityProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void validationExceptionsReturnProblemDetails() throws Exception {
        mockMvc.perform(get("/api/observability/probe/validation-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Probe validation error"))
                .andExpect(jsonPath("$.errorCode").value("MIR-VAL-PROBE"))
                .andExpect(jsonPath("$.exceptionType").value("ValidationException"));
    }
}
