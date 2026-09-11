package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.camunda.bpm.config.features.FeatureFlag;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskInitiationRequestPublisher;

import static org.slf4j.LoggerFactory.getLogger;

@Component
class EventHandlerConfiguration {

    private static final Logger LOG = getLogger(EventHandlerConfiguration.class);
    private static final String EVENT_RECEIVED_LOGGER_MESSAGE = "{} event received for task with id: {}";
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    private final TaskInitiationRequestPublisher taskInitiationRequestPublisher;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    EventHandlerConfiguration(TaskInitiationRequestPublisher taskInitiationRequestPublisher,
                              LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider) {
        this.taskInitiationRequestPublisher = taskInitiationRequestPublisher;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
    }

    @EventListener(condition = "#delegateTask.eventName=='create'")
    public void onTaskCreatedEvent(DelegateTask delegateTask) {
        LOG.info("Setting {} state to unconfigured for Task id: {}",
                CFT_TASK_STATE_LOCAL_VARIABLE_NAME,
                delegateTask.getId());
        delegateTask.setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
        if (launchDarklyFeatureFlagProvider.getBooleanValue(FeatureFlag.WA_INITIATE_TASKS_ON_CREATE)) {
            taskInitiationRequestPublisher.publishTaskInitiationRequest(delegateTask);
        }
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
