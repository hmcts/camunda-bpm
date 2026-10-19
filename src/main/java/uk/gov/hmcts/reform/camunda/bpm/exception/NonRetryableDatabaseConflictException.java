package uk.gov.hmcts.reform.camunda.bpm.exception;

import feign.FeignException;

public class NonRetryableDatabaseConflictException extends RuntimeException {

    public NonRetryableDatabaseConflictException(FeignException cause) {
        super("Task Management returned a database conflict during initiation", cause);
    }
}
