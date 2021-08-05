package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskConfigurationService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EventHandlerConfigurationTest {

    private EventHandlerConfiguration eventHandlerConfiguration;

    @Mock
    private TaskConfigurationService taskConfigurationService;

    @Before
    public void setUp() {

        eventHandlerConfiguration = new EventHandlerConfiguration(taskConfigurationService);
    }

    @Test
    public void onTaskCreatedEvent_should_call_task_configuration_service_when_flag_is_enabled() {

        DelegateTask delegateTask = mock(DelegateTask.class);
        ReflectionTestUtils.setField(eventHandlerConfiguration, "autoConfigureTaskEnabled", true);
        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);
        verify(taskConfigurationService, times(1)).configureTask(delegateTask);
    }

    @Test
    public void onTaskCreatedEvent_should_not_call_task_configuration_service_when_flag_is_disabled() {

        DelegateTask delegateTask = mock(DelegateTask.class);
        ReflectionTestUtils.setField(eventHandlerConfiguration, "autoConfigureTaskEnabled", false);
        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);
        verify(taskConfigurationService, times(0)).configureTask(delegateTask);
    }
}
