package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskInitiationRequestPublisherTest {

    private static final String TASK_ID = "task-id";

    private ApplicationEventPublisher applicationEventPublisher;
    private TaskInitiationRequestFactory taskInitiationRequestFactory;
    private TaskInitiationRequestPublisher taskInitiationRequestPublisher;

    @Before
    public void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        taskInitiationRequestFactory = mock(TaskInitiationRequestFactory.class);
        taskInitiationRequestPublisher = new TaskInitiationRequestPublisher(
            applicationEventPublisher,
            taskInitiationRequestFactory
        );
    }

    @Test
    public void should_publish_task_initiation_requested_event() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        InitiateTaskRequest request = new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication"));
        when(delegateTask.getId()).thenReturn(TASK_ID);
        when(taskInitiationRequestFactory.create(delegateTask)).thenReturn(request);

        taskInitiationRequestPublisher.publishTaskInitiationRequest(delegateTask);

        ArgumentCaptor<TaskInitiationRequestedEvent> eventCaptor =
            ArgumentCaptor.forClass(TaskInitiationRequestedEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        TaskInitiationRequestedEvent event = eventCaptor.getValue();
        assertThat(event.taskId()).isEqualTo(TASK_ID);
        assertThat(event.request()).isEqualTo(request);
    }
}
