package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskInitiationService;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EventHandlerConfigurationTest {

    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    private TaskInitiationService taskInitiationService;
    private EventHandlerConfiguration eventHandlerConfiguration;

    @Before
    public void setUp() {
        taskInitiationService = mock(TaskInitiationService.class);
        eventHandlerConfiguration = new EventHandlerConfiguration(taskInitiationService, false);
    }

    @Test
    public void should_set_task_state_to_unconfigured_when_task_is_created() {
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);

        verify(delegateTask, times(1)).setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
    }

    @Test
    public void should_not_request_task_initiation_when_feature_toggle_is_disabled() {
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);

        verify(taskInitiationService, never()).requestTaskInitiation(delegateTask);
    }

    @Test
    public void should_request_task_initiation_when_feature_toggle_is_enabled() {
        eventHandlerConfiguration = new EventHandlerConfiguration(taskInitiationService, true);
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);

        verify(taskInitiationService, times(1)).requestTaskInitiation(delegateTask);
    }

    @Test
    public void should_initiate_task_after_commit() {
        TaskInitiationRequestedEvent event = new TaskInitiationRequestedEvent(
            "task-id",
            new InitiateTaskRequest("INITIATION", Map.of())
        );

        eventHandlerConfiguration.onTaskCreatedEventAndCommit(event);

        verify(taskInitiationService, times(1)).initiateTask(event);
    }

    @Test
    public void should_set_task_state_to_pending_termination_when_task_is_completed() {
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskCompletedEvent(delegateTask);

        verify(taskInitiationService, times(1)).setTaskStateToPendingTermination("COMPLETE", delegateTask);
    }

    @Test
    public void should_set_task_state_to_pending_termination_when_task_is_deleted() {
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskDeletedEvent(delegateTask);

        verify(taskInitiationService, times(1)).setTaskStateToPendingTermination("DELETE", delegateTask);
    }
}
