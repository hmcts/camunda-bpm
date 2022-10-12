package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EventHandlerConfigurationTest {

    private EventHandlerConfiguration eventHandlerConfiguration;
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    @Before
    public void setUp() {

        eventHandlerConfiguration = new EventHandlerConfiguration();
    }

    @Test
    public void onTaskCreatedEvent_should_set_task_state_to_unconfigured() {

        DelegateTask delegateTask = mock(DelegateTask.class);
        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);
        verify(delegateTask, times(1)).setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
    }
}
