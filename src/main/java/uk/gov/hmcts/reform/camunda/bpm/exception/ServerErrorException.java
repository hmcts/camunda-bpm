package uk.gov.hmcts.reform.camunda.bpm.exception;

public class ServerErrorException extends RuntimeException {

    private static final long serialVersionUID = 6107753640554124324L;

    public ServerErrorException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
