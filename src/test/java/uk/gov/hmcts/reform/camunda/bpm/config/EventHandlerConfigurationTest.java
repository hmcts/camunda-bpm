package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.camunda.bpm.config.features.FeatureFlag;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskInitiationRequestPublisher;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventHandlerConfigurationTest {

    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    private TaskInitiationRequestPublisher taskInitiationRequestPublisher;
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private EventHandlerConfiguration eventHandlerConfiguration;

    @Before
    public void setUp() {
        taskInitiationRequestPublisher = mock(TaskInitiationRequestPublisher.class);
        launchDarklyFeatureFlagProvider = mock(LaunchDarklyFeatureFlagProvider.class);
        eventHandlerConfiguration = new EventHandlerConfiguration(
            taskInitiationRequestPublisher,
            launchDarklyFeatureFlagProvider
        );
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
        when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_INITIATE_TASKS_ON_CREATE)))
            .thenReturn(false);

        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);

        verify(taskInitiationRequestPublisher, never()).publishTaskInitiationRequest(delegateTask);
    }

    @Test
    public void should_request_task_initiation_when_feature_toggle_is_enabled() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_INITIATE_TASKS_ON_CREATE)))
            .thenReturn(true);

        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);

        verify(taskInitiationRequestPublisher, times(1)).publishTaskInitiationRequest(delegateTask);
    }

    @Test
    public void should_set_task_state_to_pending_termination_when_task_is_completed() {
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskCompletedEvent(delegateTask);

        verify(delegateTask, times(1)).setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "pendingTermination");
    }

    @Test
    public void should_set_task_state_to_pending_termination_when_task_is_deleted() {
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskDeletedEvent(delegateTask);

        verify(delegateTask, times(1)).setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "pendingTermination");
    }
}
