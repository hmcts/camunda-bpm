package uk.gov.hmcts.reform.camunda.bpm.services;

import feign.FeignException;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskInitiationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.CamundaVariable;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.TaskInitiationRequest;
import uk.gov.hmcts.reform.camunda.bpm.exception.ServerErrorException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TaskInitiationService {
    private static final Logger LOG = getLogger(TaskInitiationService.class);
    private static final String CFT_TASK_STATE = "cftTaskState";
    private static final String UNCONFIGURED = "unconfigured";

    private final AuthTokenGenerator authTokenGenerator;
    private final TaskInitiationServiceApi taskInitiationServiceApi;

    public TaskInitiationService(AuthTokenGenerator authTokenGenerator,
                                 TaskInitiationServiceApi taskInitiationServiceApi) {
        this.authTokenGenerator = authTokenGenerator;
        this.taskInitiationServiceApi = taskInitiationServiceApi;
    }

    public void pushInitiation(DelegateTask task) {
        requireNonNull(task.getId(), "taskId cannot be null");
        LOG.info("Attempting to push task initiation for task with id: {}", task.getId());
        Map<String, CamundaVariable> variables = getTypedVariables(task);
        variables.put(CFT_TASK_STATE, new CamundaVariable(UNCONFIGURED, "String"));

        try {
            taskInitiationServiceApi.pushInitiation(
                authTokenGenerator.generate(),
                task.getId(),
                new TaskInitiationRequest(
                    task.getName(),
                    task.getAssignee(),
                    toZonedDateTime(task.getCreateTime()),
                    toZonedDateTime(task.getDueDate()),
                    task.getDescription(),
                    task.getProcessInstanceId(),
                    variables
                )
            );
        } catch (FeignException ex) {
            throw new ServerErrorException(
                format("There was a problem pushing task initiation for task with id: %s", task.getId()),
                ex
            );
        }
    }

    private Map<String, CamundaVariable> getTypedVariables(DelegateTask task) {
        Map<String, CamundaVariable> variables = new HashMap<>();
        task.getVariablesTyped().forEach((key, value) -> variables.put(key, toCamundaVariable(value)));
        return variables;
    }

    private CamundaVariable toCamundaVariable(TypedValue typedValue) {
        String type = typedValue.getType() == null ? "Object" : typedValue.getType().getName();
        return new CamundaVariable(typedValue.getValue(), type);
    }

    private ZonedDateTime toZonedDateTime(java.util.Date date) {
        return date == null ? null : ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
