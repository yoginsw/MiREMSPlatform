package io.mirems.core.domain.exception;

/** Raised when a BPMN/DMN workflow or process orchestration step fails. */
public class ProcessException extends MiremsException {
    public ProcessException(String errorCode, String message) {
        super(errorCode, "Process execution failed", message);
    }

    public ProcessException(String errorCode, String message, Throwable cause) {
        super(errorCode, "Process execution failed", message, cause);
    }
}
