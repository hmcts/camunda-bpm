package uk.gov.hmcts.reform.camunda.bpm.services;

import feign.FeignException;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;

public class FeignRetryPolicy<T> {
    private static final Logger LOG = getLogger(FeignRetryPolicy.class);
    private final int maxRetries;
    private int retryCount;

    public FeignRetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    // Takes a function and executes it, if fails, passes the function to the retry command
    public T run(Supplier<T> function) {
        try {
            return function.get();
        } catch (Exception ex) {
            if (ex.getCause() != null && ex.getCause() instanceof FeignException) {
                FeignException rootException = (FeignException) ex.getCause();
                if (HttpStatus.NOT_FOUND.value() == rootException.status()) {
                    LOG.error("Non retryable exception was received, call will be aborted.");
                    return null;
                } else {
                    return retry(function);
                }
            } else {
                LOG.error(
                    "An unexpected error occurred while making a call with retry policy, call will be aborted");
                return null;
            }
        }
    }

    public int getRetryCount() {
        return retryCount;
    }

    private T retry(Supplier<T> function) {
        LOG.warn("Call failed, The call will be retried {} times.", maxRetries);
        retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                return function.get();
            } catch (Exception ex) {
                if (ex.getCause() != null && ex.getCause() instanceof FeignException) {
                    FeignException rootException = (FeignException) ex.getCause();
                    if (HttpStatus.NOT_FOUND.value() == rootException.status()) {
                        //Should not retry
                        LOG.error("Non retryable exception was received, call will be aborted.");
                        break;
                    }
                    // increment retry count and check for max retries exceed
                    retryCount++;
                    LOG.warn("[{}/{}] - Call failed on retry.", retryCount, maxRetries);
                    if (retryCount >= maxRetries) {
                        LOG.error("Maximum allowed retries exceeded.");
                        break;
                    }
                } else {
                    LOG.error(
                        "An unexpected error occurred while making a call with retry policy, call will be aborted");
                    return null;
                }
            }
        }
        return null;
    }
}
