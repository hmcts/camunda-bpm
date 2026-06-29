package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EventHandlerConfigurationTest {

    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    private TaskConfigurationServiceApi taskManagementApi;
    private AuthTokenGenerator authTokenGenerator;
    private TaskService taskService;
    private EventHandlerConfiguration eventHandlerConfiguration;

    @Before
    public void setUp() {
        taskManagementApi = mock(TaskConfigurationServiceApi.class);
        authTokenGenerator = mock(AuthTokenGenerator.class);
        taskService = mock(TaskService.class);
        eventHandlerConfiguration = new EventHandlerConfiguration(
            taskManagementApi,
            authTokenGenerator,
            taskService,
            false
        );
    }

    @Test
    public void should_set_task_state_to_unconfigured_and_not_call_task_management_when_feature_toggle_is_disabled() {
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);

        verify(delegateTask, times(1)).setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
        verify(taskManagementApi, never()).initiateTask(any(), any(), any());
    }

    @Test
    public void should_set_task_state_to_unconfigured_when_no_transaction_context() {
        eventHandlerConfiguration = new EventHandlerConfiguration(
            taskManagementApi,
            authTokenGenerator,
            taskService,
            true
        );
        DelegateTask delegateTask = mock(DelegateTask.class);

        eventHandlerConfiguration.onTaskCreatedEvent(delegateTask);

        verify(delegateTask, times(1)).setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
        verify(taskManagementApi, never()).initiateTask(any(), any(), any());
    }
}
