package uk.gov.hmcts.reform.camunda.bpm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.camunda.bpm.exception.NonRetryableDatabaseConflictException;

@Service
public class TaskInitiationRetryService {

    private static final String DATABASE_CONFLICT_TYPE =
        "https://github.com/hmcts/wa-task-management-api/problem/database-conflict";

    private final TaskConfigurationServiceApi taskManagementApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final ObjectMapper objectMapper;

    public TaskInitiationRetryService(TaskConfigurationServiceApi taskManagementApi,
                                      AuthTokenGenerator authTokenGenerator,
                                      ObjectMapper objectMapper) {
        this.taskManagementApi = taskManagementApi;
        this.authTokenGenerator = authTokenGenerator;
        this.objectMapper = objectMapper;
    }

    @Retryable(
        retryFor = FeignException.class,
        noRetryFor = NonRetryableDatabaseConflictException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public void initiateTaskWithRetry(String taskId, InitiateTaskRequest request) {
        try {
            taskManagementApi.initiateTask(authTokenGenerator.generate(), taskId, request);
        } catch (FeignException exception) {
            if (isDatabaseConflict(exception)) {
                throw new NonRetryableDatabaseConflictException(exception);
            }
            throw exception;
        }
    }

    private boolean isDatabaseConflict(FeignException exception) {
        if (exception.status() != 503) {
            return false;
        }
        try {
            return DATABASE_CONFLICT_TYPE.equals(
                objectMapper.readTree(exception.contentUTF8()).path("type").asText()
            );
        } catch (JsonProcessingException ignored) {
            return false;
        }
    }

}
