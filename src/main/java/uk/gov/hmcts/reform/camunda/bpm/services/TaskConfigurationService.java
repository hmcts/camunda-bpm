package uk.gov.hmcts.reform.camunda.bpm.services;

import feign.FeignException;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.ConfigureTaskRequest;
import uk.gov.hmcts.reform.camunda.bpm.domain.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.camunda.bpm.exception.ServerErrorException;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Service
public class TaskConfigurationService {

    private static final int MAX_RETRIES = 3;
    private final TaskConfigurationServiceApi taskConfigurationServiceApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final FeignRetryPolicy<ConfigureTaskResponse> withFeignRetryPolicy;


    @Autowired
    public TaskConfigurationService(AuthTokenGenerator authTokenGenerator,
                                    TaskConfigurationServiceApi taskConfigurationServiceApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.taskConfigurationServiceApi = taskConfigurationServiceApi;
        withFeignRetryPolicy = new FeignRetryPolicy<>(MAX_RETRIES);
    }

    public void configureTask(DelegateTask task) {
        requireNonNull(task.getId(), "taskId cannot be null");
        Map<String, Object> variables = task.getVariables();

        ConfigureTaskResponse response = withFeignRetryPolicy.run(
            () -> performConfigureTaskAction(task.getId(), new ConfigureTaskRequest(variables)
            ));


        // If the call resulted in a non-retryable exception update the task state to unconfigured only.
        if (response == null) {
            task.setVariable("taskState", "unconfigured");
        } else {

            /*
              Merge old original variables with the new ones from the response.
              Favouring the response process variables as they might contain updates.
            */
            Map<String, Object> mergedVariables = Stream.concat(
                variables.entrySet().stream(),
                response.getConfigurationVariables().entrySet().stream())
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue.equals(newValue) ? oldValue : newValue)
                );

            // If response contained an assignee also update mutable object's assignee
            if (response.getAssignee() != null) {
                task.setAssignee(response.getAssignee());
            }
            // Update task mutable object with merged process variables
            task.setVariables(mergedVariables);
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
                String.format(
                    "There was a problem configuring the task with id: %s",
                    taskId
                ), ex);
        }
    }
}
