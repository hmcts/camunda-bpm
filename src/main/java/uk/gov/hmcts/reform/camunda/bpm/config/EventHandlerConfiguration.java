package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskConfigurationService;

import static org.slf4j.LoggerFactory.getLogger;


@Component
class EventHandlerConfiguration {

    private static final Logger LOG = getLogger(EventHandlerConfiguration.class);
    private final TaskConfigurationService taskConfigurationService;
    @Value("${configuration.autoConfigureTasks}")
    private boolean autoConfigureTaskEnabled;

    public EventHandlerConfiguration(TaskConfigurationService taskConfigurationService) {
        this.taskConfigurationService = taskConfigurationService;
    }

    @EventListener(condition = "#delegateTask.eventName=='create'")
    public void onTaskCreatedEvent(DelegateTask delegateTask) {
        if (autoConfigureTaskEnabled) {
            LOG.info(
                "Create event received, attempting to configure task with id: {}",
                delegateTask.getId()
            );
            /*
            Uses DelegateTask as it is a mutable object
            Call wa-task-configuration to retrieve configuration for a tasks.
            The reason it is done in this way is because the tasks does not yet exist in the database
            when this event is triggered
            */

            taskConfigurationService.configureTask(delegateTask);
        } else {
            LOG.info(
                "Create event received. Event processed but not handled. auto configuration flag was disabled"
            );
        }
    }

    @EventListener(condition = "#delegateTask.eventName=='complete'")
    public void onTaskCompletedEvent(DelegateTask delegateTask) {
        LOG.info("Complete event received for task with id: {}", delegateTask.getId());
        delegateTask.setVariableLocal("cftTaskState", "pendingTermination");
    }

    @EventListener(condition = "#delegateTask.eventName=='delete'")
    public void onTaskDeletedEvent(DelegateTask delegateTask) {
        LOG.info("Delete event received for task with id: {}", delegateTask.getId());
        delegateTask.setVariableLocal("cftTaskState", "pendingTermination");
    }

}
