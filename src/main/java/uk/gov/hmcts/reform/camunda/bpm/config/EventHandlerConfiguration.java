package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.impl.cfg.TransactionContext;
import org.camunda.bpm.engine.impl.cfg.TransactionState;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Component
class EventHandlerConfiguration {

    private static final Logger LOG = getLogger(EventHandlerConfiguration.class);
    private static final String EVENT_RECEIVED_LOGGER_MESSAGE = "{} event received for task with id: {}";
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";
    private static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Autowired
    private TaskConfigurationServiceApi taskManagementApi;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @EventListener(condition = "#delegateTask.eventName=='create'")
    public void onTaskCreatedEvent(DelegateTask delegateTask) {
        LOG.info("Setting {} state to unconfigured for Task id: {}",
                 CFT_TASK_STATE_LOCAL_VARIABLE_NAME,
                 delegateTask.getId());
        delegateTask.setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
        scheduleInitiateTask(delegateTask);
    }

    @EventListener(condition = "#delegateTask.eventName=='complete'")
    public void onTaskCompletedEvent(DelegateTask delegateTask) {
        LOG.info(EVENT_RECEIVED_LOGGER_MESSAGE, "COMPLETE", delegateTask.getId());
        delegateTask.setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "pendingTermination");
    }

    @EventListener(condition = "#delegateTask.eventName=='delete'")
    public void onTaskDeletedEvent(DelegateTask delegateTask) {
        LOG.info(EVENT_RECEIVED_LOGGER_MESSAGE, "DELETE", delegateTask.getId());
        delegateTask.setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "pendingTermination");
    }

    private void scheduleInitiateTask(DelegateTask delegateTask) {
        String taskId = delegateTask.getId();
        Map<String, Object> request = Map.of(
            "operation",
            "INITIATION",
            "task_attributes",
            taskAttributes(delegateTask)
        );

        scheduleAfterCamundaCommit(taskId, request);
    }

    private void scheduleAfterCamundaCommit(String taskId, Map<String, Object> request) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null || commandContext.getTransactionContext() == null) {
            LOG.warn(
                "Task id: {} could not be scheduled for initiation because there is no Camunda transaction context.",
                taskId
            );
            return;
        }

        TransactionContext transactionContext = commandContext.getTransactionContext();
        transactionContext.addTransactionListener(
            TransactionState.COMMITTED,
            ignoredCommandContext -> initiateTask(taskId, request)
        );
        LOG.info("Task id: {} scheduled to push to Task Management after Camunda commit", taskId);
    }

    private void initiateTask(String taskId, Map<String, Object> request) {
        try {
            taskManagementApi.initiateTask(
                authTokenGenerator.generate(),
                taskId,
                request
            );
            LOG.info("Task id: {} pushed to Task Management for initiation", taskId);
        } catch (Exception ex) {
            LOG.warn(
                "Task id: {} could not be pushed for initiation. Task monitor fallback remains available.",
                taskId,
                ex
            );
        }
    }

    private Map<String, Object> taskAttributes(DelegateTask delegateTask) {
        Map<String, Object> variables = delegateTask.getVariables();
        Map<String, Object> attributes = variables == null ? new HashMap<>() : new HashMap<>(variables);
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
