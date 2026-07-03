package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TaskInitiationService {

    private static final Logger LOG = getLogger(TaskInitiationService.class);
    private static final String EVENT_RECEIVED_LOGGER_MESSAGE = "{} event received for task with id: {}";
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";
    private static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final TaskConfigurationServiceApi taskManagementApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final TaskService taskService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskExecutor taskInitiationExecutor;

    public TaskInitiationService(TaskConfigurationServiceApi taskManagementApi,
                                 AuthTokenGenerator authTokenGenerator,
                                 TaskService taskService,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 @Qualifier("taskInitiationExecutor") TaskExecutor taskInitiationExecutor) {
        this.taskManagementApi = taskManagementApi;
        this.authTokenGenerator = authTokenGenerator;
        this.taskService = taskService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.taskInitiationExecutor = taskInitiationExecutor;
    }

    private void setTaskStateToUnconfigured(String taskId) {
        try {
            taskService.setVariableLocal(
                taskId,
                CFT_TASK_STATE_LOCAL_VARIABLE_NAME,
                "unconfigured"
            );
        } catch (Exception ex) {
            LOG.error("Task id: {} could not be updated to unconfigured after initiation failure.", taskId, ex);
        }
    }

    public void setTaskStateToPendingTermination(String eventName, DelegateTask delegateTask) {
        LOG.info(EVENT_RECEIVED_LOGGER_MESSAGE, eventName, delegateTask.getId());
        delegateTask.setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "PENDING_TERMINATION_TASK_STATE");
    }

    public void requestTaskInitiation(DelegateTask delegateTask) {
        String taskId = delegateTask.getId();
        InitiateTaskRequest request = new InitiateTaskRequest(
            "INITIATION",
            taskAttributes(delegateTask)
        );

        applicationEventPublisher.publishEvent(new TaskInitiationRequestedEvent(taskId, request));
    }

    public void initiateTask(TaskInitiationRequestedEvent event) {
        try {
            taskInitiationExecutor.execute(() -> sendInitiationRequestToTaskManagement(event));
        } catch (TaskRejectedException ex) {
            LOG.warn(
                "Task id: {} could not be queued for initiation. Task state will remain unconfigured.",
                event.taskId(),
                ex
            );
            setTaskStateToUnconfigured(event.taskId());
        }
    }

    private void sendInitiationRequestToTaskManagement(TaskInitiationRequestedEvent event) {
        try {
            taskManagementApi.initiateTask(
                authTokenGenerator.generate(),
                event.taskId(),
                event.request()
            );
            LOG.info("Task id: {} pushed to Task Management for initiation", event.taskId());
        } catch (Exception ex) {
            LOG.warn(
                "Task id: {} could not be pushed for initiation. Task state will be set to unconfigured.",
                event.taskId(),
                ex
            );
            setTaskStateToUnconfigured(event.taskId());
        }
    }

    private Map<String, Object> taskAttributes(DelegateTask delegateTask) {
        Map<String, Object> variables = delegateTask.getVariables();
        Map<String, Object> attributes = variables == null ? new HashMap<>() : new HashMap<>(variables);

        attributes.remove("assignee");
        attributes.remove("description");
        attributes.remove("dueDate");
        attributes.remove("name");
        attributes.remove("priorityDate");

        Object taskType = attributes.get("taskType") != null ? attributes.get("taskType") : attributes.get("taskId");
        attributes.put("taskType", taskType);
        attributes.put("name", delegateTask.getName());

        putIfNotNull(attributes, "dueDate", format(delegateTask.getDueDate()));
        putIfNotNull(attributes, "created", format(delegateTask.getCreateTime()));
        putIfNotNull(attributes, "assignee", delegateTask.getAssignee());
        putIfNotNull(attributes, "description", delegateTask.getDescription());

        return attributes;
    }

    private void putIfNotNull(Map<String, Object> attributes, String name, Object value) {
        if (value != null) {
            attributes.put(name, value);
        }
    }

    private String format(Date date) {
        if (date == null) {
            return null;
        }
        return CAMUNDA_DATA_TIME_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }
}
