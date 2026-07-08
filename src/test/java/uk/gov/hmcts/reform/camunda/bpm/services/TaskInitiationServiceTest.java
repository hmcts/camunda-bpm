package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.TaskService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskInitiationServiceTest {

    private static final String SERVICE_TOKEN = "S2S_TOKEN";
    private static final String TASK_ID = "task-id";
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    private TaskConfigurationServiceApi taskManagementApi;
    private AuthTokenGenerator authTokenGenerator;
    private TaskService taskService;
    private TaskExecutor taskInitiationExecutor;
    private TaskInitiationService taskInitiationService;

    @Before
    public void setUp() {
        taskManagementApi = mock(TaskConfigurationServiceApi.class);
        authTokenGenerator = mock(AuthTokenGenerator.class);
        taskService = mock(TaskService.class);
        taskInitiationExecutor = new SyncTaskExecutor();
        taskInitiationService = new TaskInitiationService(
            taskManagementApi,
            authTokenGenerator,
            taskService,
            taskInitiationExecutor
        );
    }

    @Test
    public void should_initiate_task_from_requested_event() {
        InitiateTaskRequest request = new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication"));
        when(authTokenGenerator.generate()).thenReturn(SERVICE_TOKEN);

        taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(TASK_ID, request));

        verify(taskManagementApi, times(1)).initiateTask(SERVICE_TOKEN, TASK_ID, request);
    }

    @Test
    public void should_set_task_state_to_unconfigured_when_initiation_fails() {
        InitiateTaskRequest request = new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication"));
        when(authTokenGenerator.generate()).thenReturn(SERVICE_TOKEN);
        doThrow(new RuntimeException("Task Management unavailable"))
            .when(taskManagementApi)
            .initiateTask(eq(SERVICE_TOKEN), eq(TASK_ID), any(InitiateTaskRequest.class));

        taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(TASK_ID, request));

        verify(taskService, times(1)).setVariableLocal(TASK_ID, CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
    }

    @Test
    public void should_set_task_state_to_unconfigured_when_initiation_is_rejected_by_executor() {
        final InitiateTaskRequest request = new InitiateTaskRequest(
            "INITIATION",
            Map.of("taskType", "processApplication")
        );
        taskInitiationExecutor = mock(TaskExecutor.class);
        doThrow(new TaskRejectedException("Task initiation queue is full"))
            .when(taskInitiationExecutor)
            .execute(any(Runnable.class));
        taskInitiationService = new TaskInitiationService(
            taskManagementApi,
            authTokenGenerator,
            taskService,
            taskInitiationExecutor
        );

        taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(TASK_ID, request));

        verify(taskService, times(1)).setVariableLocal(TASK_ID, CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
    }
}
