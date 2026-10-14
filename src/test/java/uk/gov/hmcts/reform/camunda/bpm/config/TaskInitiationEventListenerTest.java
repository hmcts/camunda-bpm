package uk.gov.hmcts.reform.camunda.bpm.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskInitiationService;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TaskInitiationEventListenerTest {

    private TaskInitiationService taskInitiationService;
    private TaskInitiationEventListener taskInitiationEventListener;

    @Before
    public void setUp() {
        taskInitiationService = mock(TaskInitiationService.class);
        taskInitiationEventListener = new TaskInitiationEventListener(taskInitiationService);
    }

    @Test
    public void should_initiate_task_after_commit_event_is_received() {
        TaskInitiationRequestedEvent event = new TaskInitiationRequestedEvent(
            "task-id",
            new InitiateTaskRequest("INITIATION", Map.of())
        );

        taskInitiationEventListener.onTaskInitiationRequested(event);

        verify(taskInitiationService, times(1)).initiateTask(event);
    }
}
