package uk.gov.hmcts.reform.camunda.bpm.services;

import feign.FeignException;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.ConfigureTaskRequest;
import uk.gov.hmcts.reform.camunda.bpm.domain.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.camunda.bpm.exception.ServerErrorException;

import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TaskConfigurationService {
    private static final Logger LOG = getLogger(TaskConfigurationService.class);

    private final TaskConfigurationServiceApi taskConfigurationServiceApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final FeignRetryPolicy<ConfigureTaskResponse> withFeignRetryPolicy;


    @Autowired
    public TaskConfigurationService(
        @Value("${configuration.maxRetries}") int maxRetries,
        AuthTokenGenerator authTokenGenerator,
        TaskConfigurationServiceApi taskConfigurationServiceApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.taskConfigurationServiceApi = taskConfigurationServiceApi;
        withFeignRetryPolicy = new FeignRetryPolicy<>(maxRetries);
    }

    public void configureTask(DelegateTask task) {
        requireNonNull(task.getId(), "taskId cannot be null");
        Map<String, Object> variables = task.getVariables();

        ConfigureTaskResponse response = withFeignRetryPolicy.run(
            () -> performConfigureTaskAction(task.getId(), new ConfigureTaskRequest(variables)
            ));

        // If the call resulted in a non-retryable exception update task state to unconfigured only.
        if (response == null) {
            task.setVariableLocal("taskState", "unconfigured");
            LOG.warn(
                "Task could not be configured. Task state was set to 'unconfigured' for task id: {}",
                task.getId()
            );
        } else {
            // If response contained an assignee also update mutable object's assignee
            if (response.getAssignee() != null) {
                task.setAssignee(response.getAssignee());
            }

            // Update all new variables as local variables scope
            task.setVariablesLocal(response.getConfigurationVariables());
        }
    }


    private ConfigureTaskResponse performConfigureTaskAction(String taskId, ConfigureTaskRequest configureTaskRequest) {
        try {
            return taskConfigurationServiceApi.configureTask(
                authTokenGenerator.generate(),
                taskId,
                configureTaskRequest
            );
        } catch (FeignException ex) {
            throw new ServerErrorException(
                format(
                    "There was a problem configuring the task with id: %s",
                    taskId
                ), ex);
        }
    }
}
