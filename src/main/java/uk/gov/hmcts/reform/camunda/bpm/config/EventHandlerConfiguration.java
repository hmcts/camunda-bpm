package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
class EventHandlerConfiguration {

    private static final Logger LOG = getLogger(EventHandlerConfiguration.class);
    private static final String EVENT_RECEIVED_LOGGER_MESSAGE = "{} event received for task with id: {}";
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    @EventListener(condition = "#delegateTask.eventName=='create'")
    public void onTaskCreatedEvent(DelegateTask delegateTask) {
        delegateTask.setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
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

}
