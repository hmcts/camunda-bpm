package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskConfigurationService;

@Component
class EventHandlerConfiguration {

    private static final String TEST_PURPOSES_ASSIGNEE_ID = "demo";

    /*
     This assignee id is used as a blacklist
     to ignore tasks created with this assignee usually created for testing purposes
     */
    private final TaskConfigurationService taskConfigurationService;

    @Value("${configuration.autoConfigureTasks}")
    private boolean autoConfigureTaskEnabled;

    public EventHandlerConfiguration(TaskConfigurationService taskConfigurationService) {
        this.taskConfigurationService = taskConfigurationService;
    }

    @EventListener
    public void onTaskCreatedEvent(DelegateTask delegateTask) {

        if (autoConfigureTaskEnabled) {
            // Avoid the first 2 demo tasks that get created for testing purposes on application startup
            if (!TEST_PURPOSES_ASSIGNEE_ID.equals(delegateTask.getAssignee())) {

            /*
             Uses DelegateTask as it is a mutable object
             Call wa-task-configuration to retrieve configuration for a tasks.
             The reason it is done in this way is because the tasks does not yet exist in the database
             when this event is triggered
             */
                taskConfigurationService.configureTask(delegateTask);
            }
        }
    }

}
