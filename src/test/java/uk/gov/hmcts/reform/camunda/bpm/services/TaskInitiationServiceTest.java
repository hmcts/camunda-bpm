package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.TaskService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.util.Map;
import java.util.concurrent.CyclicBarrier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class TaskInitiationServiceTest {

    private static final String TASK_ID = "task-id";
    private static final String CFT_TASK_STATE_LOCAL_VARIABLE_NAME = "cftTaskState";

    private TaskInitiationRetryService taskInitiationRetryService;
    private TaskService taskService;
    private TaskExecutor taskInitiationExecutor;
    private TaskInitiationService taskInitiationService;

    @Before
    public void setUp() {
        taskInitiationRetryService = mock(TaskInitiationRetryService.class);
        taskService = mock(TaskService.class);
        taskInitiationExecutor = new SyncTaskExecutor();
        taskInitiationService = new TaskInitiationService(
            taskInitiationRetryService,
            taskService,
            taskInitiationExecutor
        );
    }

    @Test
    public void should_initiate_task_from_requested_event() {
        InitiateTaskRequest request = new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication"));
        taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(TASK_ID, request));

        verify(taskInitiationRetryService, times(1)).initiateTaskWithRetry(TASK_ID, request);
    }

    @Test
    public void should_set_task_state_to_unconfigured_when_initiation_fails() {
        InitiateTaskRequest request = new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication"));
        doThrow(new RuntimeException("Task Management unavailable"))
            .when(taskInitiationRetryService)
            .initiateTaskWithRetry(TASK_ID, request);

        taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(TASK_ID, request));

        verify(taskService, times(1)).setVariableLocal(TASK_ID, CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
    }

    @Test
    public void should_handle_failure_when_task_state_cannot_be_set_to_unconfigured() {
        InitiateTaskRequest request = new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication"));
        doThrow(new RuntimeException("Task Management unavailable"))
            .when(taskInitiationRetryService)
            .initiateTaskWithRetry(TASK_ID, request);
        doThrow(new RuntimeException("Camunda task unavailable"))
            .when(taskService)
            .setVariableLocal(TASK_ID, CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");

        assertThatCode(() ->
            taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(TASK_ID, request))
        ).doesNotThrowAnyException();

        verify(taskService).setVariableLocal(TASK_ID, CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
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
            taskInitiationRetryService,
            taskService,
            taskInitiationExecutor
        );

        taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(TASK_ID, request));

        verify(taskService, times(1)).setVariableLocal(TASK_ID, CFT_TASK_STATE_LOCAL_VARIABLE_NAME, "unconfigured");
    }

    @Test
    public void should_process_multiple_task_initiation_requests_concurrently() throws Exception {
        final int numberOfTasks = 4;
        final InitiateTaskRequest request = new InitiateTaskRequest(
            "INITIATION",
            Map.of("taskType", "processApplication")
        );
        final CyclicBarrier allTasksStarted = new CyclicBarrier(numberOfTasks);
        final ThreadPoolTaskExecutor concurrentExecutor = concurrentTaskInitiationExecutor(numberOfTasks);
        taskInitiationService = new TaskInitiationService(
            taskInitiationRetryService,
            taskService,
            concurrentExecutor
        );
        doAnswer(invocation -> {
            allTasksStarted.await(10, SECONDS);
            return null;
        }).when(taskInitiationRetryService).initiateTaskWithRetry(anyString(), any(InitiateTaskRequest.class));

        try {
            for (int i = 0; i < numberOfTasks; i++) {
                taskInitiationService.initiateTask(new TaskInitiationRequestedEvent("task-id-" + i, request));
            }

            await().atMost(10, SECONDS).untilAsserted(() ->
                verify(taskInitiationRetryService, times(numberOfTasks))
                    .initiateTaskWithRetry(anyString(), any(InitiateTaskRequest.class))
            );
        } finally {
            concurrentExecutor.shutdown();
        }
    }

    @Test
    public void should_preserve_task_and_request_pairing_during_concurrent_initiation() throws Exception {
        final Map<String, InitiateTaskRequest> taskRequests = concurrentTaskRequests();
        final CyclicBarrier allTasksStarted = new CyclicBarrier(taskRequests.size());
        final ThreadPoolTaskExecutor concurrentExecutor = concurrentTaskInitiationExecutor(taskRequests.size());
        taskInitiationService = new TaskInitiationService(
            taskInitiationRetryService,
            taskService,
            concurrentExecutor
        );
        doAnswer(invocation -> {
            allTasksStarted.await(10, SECONDS);
            return null;
        }).when(taskInitiationRetryService).initiateTaskWithRetry(anyString(), any(InitiateTaskRequest.class));

        try {
            taskRequests.forEach((taskId, request) ->
                taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(taskId, request))
            );

            await().atMost(10, SECONDS).untilAsserted(() ->
                taskRequests.forEach((taskId, request) ->
                    verify(taskInitiationRetryService, times(1)).initiateTaskWithRetry(taskId, request)
                )
            );
            verifyNoMoreInteractions(taskInitiationRetryService);
        } finally {
            concurrentExecutor.shutdown();
        }
    }

    @Test
    public void should_isolate_failure_when_concurrent_task_initiation_fails() throws Exception {
        final String failingTaskId = "task-id-2";
        final Map<String, InitiateTaskRequest> taskRequests = concurrentTaskRequests();
        final CyclicBarrier allTasksStarted = new CyclicBarrier(taskRequests.size());
        final ThreadPoolTaskExecutor concurrentExecutor = concurrentTaskInitiationExecutor(taskRequests.size());
        taskInitiationService = new TaskInitiationService(
            taskInitiationRetryService,
            taskService,
            concurrentExecutor
        );
        doAnswer(invocation -> {
            allTasksStarted.await(10, SECONDS);
            if (failingTaskId.equals(invocation.getArgument(0))) {
                throw new RuntimeException("Task Management unavailable");
            }
            return null;
        }).when(taskInitiationRetryService).initiateTaskWithRetry(anyString(), any(InitiateTaskRequest.class));

        try {
            taskRequests.forEach((taskId, request) ->
                taskInitiationService.initiateTask(new TaskInitiationRequestedEvent(taskId, request))
            );

            await().atMost(10, SECONDS).untilAsserted(() -> {
                taskRequests.forEach((taskId, request) ->
                    verify(taskInitiationRetryService, times(1)).initiateTaskWithRetry(taskId, request)
                );
                verify(taskService, times(1)).setVariableLocal(
                    failingTaskId,
                    CFT_TASK_STATE_LOCAL_VARIABLE_NAME,
                    "unconfigured"
                );
            });
            verifyNoMoreInteractions(taskInitiationRetryService, taskService);
        } finally {
            concurrentExecutor.shutdown();
        }
    }

    private Map<String, InitiateTaskRequest> concurrentTaskRequests() {
        return Map.of(
            "task-id-0", new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication-0")),
            "task-id-1", new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication-1")),
            "task-id-2", new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication-2")),
            "task-id-3", new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication-3"))
        );
    }

    private ThreadPoolTaskExecutor concurrentTaskInitiationExecutor(int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(poolSize);
        executor.initialize();
        return executor;
    }
}
