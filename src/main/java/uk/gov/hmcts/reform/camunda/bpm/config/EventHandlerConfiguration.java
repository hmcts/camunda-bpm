package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskConfigurationService;

@Component
class EventHandlerConfiguration {

    /*
     This assignee id is used as a blacklist
     to ignore tasks created with this assignee usually created for testing purposes
     */

    private static final String TEST_PURPOSES_ASSIGNEE_ID = "demo";
    private final TaskConfigurationService taskConfigurationService;

    public EventHandlerConfiguration(TaskConfigurationService taskConfigurationService) {
        this.taskConfigurationService = taskConfigurationService;
    }

    @EventListener
    public void onTaskCreatedEvent(DelegateTask delegateTask) {

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
