package uk.gov.hmcts.reform.camunda.bpm.services;

import feign.FeignException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

@Service
public class TaskInitiationRetryService {

    private final TaskConfigurationServiceApi taskManagementApi;
    private final AuthTokenGenerator authTokenGenerator;

    public TaskInitiationRetryService(TaskConfigurationServiceApi taskManagementApi,
                                      AuthTokenGenerator authTokenGenerator) {
        this.taskManagementApi = taskManagementApi;
        this.authTokenGenerator = authTokenGenerator;
    }

    @Retryable(retryFor = FeignException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void initiateTaskWithRetry(String taskId, InitiateTaskRequest request) {
        taskManagementApi.initiateTask(authTokenGenerator.generate(), taskId, request);
    }
}
