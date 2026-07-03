package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
    private ApplicationEventPublisher applicationEventPublisher;
    private TaskExecutor taskInitiationExecutor;
    private TaskInitiationService taskInitiationService;

    @Before
    public void setUp() {
        taskManagementApi = mock(TaskConfigurationServiceApi.class);
        authTokenGenerator = mock(AuthTokenGenerator.class);
        taskService = mock(TaskService.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        taskInitiationExecutor = new SyncTaskExecutor();
        taskInitiationService = new TaskInitiationService(
            taskManagementApi,
            authTokenGenerator,
            taskService,
            applicationEventPublisher,
            taskInitiationExecutor
        );
    }

    @Test
    public void should_set_task_state_to_pending_termination_on_delegate_task() {
        DelegateTask delegateTask = mock(DelegateTask.class);

        taskInitiationService.setTaskStateToPendingTermination("COMPLETE", delegateTask);

        verify(delegateTask, times(1)).setVariableLocal(CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "pendingTermination");
    }

    @Test
    public void should_publish_task_initiation_requested_event_with_task_attributes() {
        DelegateTask delegateTask = delegateTask();
        ArgumentCaptor<TaskInitiationRequestedEvent> eventCaptor =
            ArgumentCaptor.forClass(TaskInitiationRequestedEvent.class);

        taskInitiationService.requestTaskInitiation(delegateTask);

        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        TaskInitiationRequestedEvent event = eventCaptor.getValue();
        InitiateTaskRequest request = event.request();
        assertThat(event.taskId()).isEqualTo(TASK_ID);
        assertThat(request.getOperation()).isEqualTo("INITIATION");
        assertThat(request.getTaskAttributes())
            .containsEntry("caseId", "1678901234567890")
            .containsEntry("caseTypeId", "WaCaseType")
            .containsEntry("jurisdiction", "WA")
            .containsEntry("name", "Process Application")
            .containsEntry("taskType", "processApplication");
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
            applicationEventPublisher,
            taskInitiationExecutor
        );

        taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(TASK_ID, request));

        verify(taskService, times(1)).setVariableLocal(TASK_ID, CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
    }

    private DelegateTask delegateTask() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getId()).thenReturn(TASK_ID);
        when(delegateTask.getName()).thenReturn("Process Application");
        when(delegateTask.getVariables()).thenReturn(Map.of(
            "caseId", "1678901234567890",
            "caseTypeId", "WaCaseType",
            "jurisdiction", "WA",
            "taskId", "processApplication"
        ));
        return delegateTask;
    }
}
