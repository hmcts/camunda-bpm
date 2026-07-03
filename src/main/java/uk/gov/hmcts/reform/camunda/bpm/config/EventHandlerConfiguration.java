package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskInitiationService;

import static org.slf4j.LoggerFactory.getLogger;

@Component
class EventHandlerConfiguration {

    private static final Logger LOG = getLogger(EventHandlerConfiguration.class);
    private static final String EVENT_RECEIVED_LOGGER_MESSAGE = "{} event received for task with id: {}";
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    private final TaskInitiationService taskInitiationService;
    private final boolean initiateTasksOnCreate;

    EventHandlerConfiguration(TaskInitiationService taskInitiationService,
                              @Value("${configuration.initiateTasksOnCreate:false}") boolean initiateTasksOnCreate) {
        this.taskInitiationService = taskInitiationService;
        this.initiateTasksOnCreate = initiateTasksOnCreate;
    }

    @EventListener(condition = "#delegateTask.eventName=='create'")
    public void onTaskCreatedEvent(DelegateTask delegateTask) {
        LOG.info("Setting {} state to unconfigured for Task id: {}",
                CFT_TASK_STATE_LOCAL_VARIABLE_NAME,
                delegateTask.getId());
        delegateTask.setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
        if (initiateTasksOnCreate) {
            taskInitiationService.requestTaskInitiation(delegateTask);
        }
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void onTaskCreatedEventAndCommit(TaskInitiationRequestedEvent event) {
        taskInitiationService.initiateTask(event);
    }

    @EventListener(condition = "#delegateTask.eventName=='complete'")
    public void onTaskCompletedEvent(DelegateTask delegateTask) {
        taskInitiationService.setTaskStateToPendingTermination("COMPLETE", delegateTask);
    }

    @EventListener(condition = "#delegateTask.eventName=='delete'")
    public void onTaskDeletedEvent(DelegateTask delegateTask) {
        taskInitiationService.setTaskStateToPendingTermination("DELETE", delegateTask);
    }

}
