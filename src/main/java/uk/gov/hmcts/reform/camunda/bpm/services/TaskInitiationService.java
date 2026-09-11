package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.TaskService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TaskInitiationService {

    private static final Logger LOG = getLogger(TaskInitiationService.class);
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    private final TaskInitiationRetryService taskInitiationRetryService;
    private final TaskService taskService;
    private final TaskExecutor taskInitiationExecutor;

    public TaskInitiationService(TaskInitiationRetryService taskInitiationRetryService,
                                 TaskService taskService,
                                 @Qualifier("taskInitiationExecutor") TaskExecutor taskInitiationExecutor) {
        this.taskInitiationRetryService = taskInitiationRetryService;
        this.taskService = taskService;
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
            taskInitiationRetryService.initiateTaskWithRetry(event.taskId(), event.request());
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
}
