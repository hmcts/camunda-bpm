package uk.gov.hmcts.reform.camunda.bpm.services;

import feign.FeignException;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.camunda.bpm.domain.response.ConfigureTaskResponse;

import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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
            if (isRetryableException(ex)) {
                return retry(function);
            } else {
                logForUnconfiguredTasks(function);
                LOG.error("Non retryable exception was received, call will be aborted. Exception message was: {}",
                        ex.getMessage());
                return null;
            }
        }
    }

    public void logForUnconfiguredTasks(Supplier<T> function) {
        if (function instanceof ConfigureTaskResponse) {

            LOG.error("Task with ID: '{}' could not be configured. Related Case ID: {} Full Variable list: {}",
                    ((ConfigureTaskResponse) function).getTaskId(),
                    ((ConfigureTaskResponse) function).getCaseId(),
                    ((ConfigureTaskResponse) function).getConfigurationVariables().toString());
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
                if (isRetryableException(ex)) {
                    // increment retry count and check for max retries exceed
                    retryCount++;
                    LOG.warn("[{}/{}] - Call failed on retry.", retryCount, maxRetries);
                    if (retryCount >= maxRetries) {
                        LOG.error("Maximum allowed retries exceeded.", ex);
                        break;
                    }
                } else {
                    LOG.error("Non retryable exception was received, call will be aborted.");
                    break;
                }
            }
        }
        return null;
    }

    private boolean isRetryableException(Exception ex) {
        if (ex.getCause() != null && ex.getCause() instanceof FeignException) {
            FeignException rootException = (FeignException) ex.getCause();
            if (rootException.status() != NOT_FOUND.value()) {
                // Can retry exception
                return true;
            }
        }
        //Should not retry
        return false;
    }
}
