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

    private static final String TEST_PURPOSES_ASSIGNEE_ID = "demo";
    private static final String CREATE_EVENT = "create";
    private static final Logger LOG = getLogger(EventHandlerConfiguration.class);
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

        if (CREATE_EVENT.equals(delegateTask.getEventName())) {
            if (autoConfigureTaskEnabled) {
                // Avoid the first 2 demo tasks that get created for testing purposes on application startup
                if (!TEST_PURPOSES_ASSIGNEE_ID.equals(delegateTask.getAssignee())) {

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
                }
            } else {
                LOG.info(
                    "Create event received. Event processed but not handled. auto configuration flag was disabled"
                );
            }
        }
    }
}
