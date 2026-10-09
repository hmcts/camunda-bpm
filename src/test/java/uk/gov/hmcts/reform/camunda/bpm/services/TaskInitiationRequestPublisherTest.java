package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.repository.ProcessDefinition;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskInitiationRequestPublisherTest {

    private static final String TASK_ID = "task-id";

    private ApplicationEventPublisher applicationEventPublisher;
    private TaskInitiationRequestFactory taskInitiationRequestFactory;
    private RepositoryService repositoryService;
    private TaskInitiationRequestPublisher taskInitiationRequestPublisher;

    @Before
    public void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        taskInitiationRequestFactory = mock(TaskInitiationRequestFactory.class);
        repositoryService = mock(RepositoryService.class);
        taskInitiationRequestPublisher = new TaskInitiationRequestPublisher(
            applicationEventPublisher,
            taskInitiationRequestFactory,
            repositoryService
        );
    }

    @Test
    public void should_publish_task_initiation_requested_event() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        InitiateTaskRequest request = new InitiateTaskRequest("INITIATION", Map.of("taskType", "processApplication"));
        when(delegateTask.getTaskDefinitionKey()).thenReturn("processTask");
        when(delegateTask.getProcessDefinitionId()).thenReturn("process-definition-id");
        when(repositoryService.getProcessDefinition("process-definition-id")).thenReturn(processDefinition);
        when(processDefinition.getKey()).thenReturn("wa-task-initiation-ia-asylum");
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

    @Test
    public void should_not_publish_for_another_task_definition() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getTaskDefinitionKey()).thenReturn("otherTask");

        taskInitiationRequestPublisher.publishTaskInitiationRequest(delegateTask);

        verify(repositoryService, never()).getProcessDefinition("process-definition-id");
        verify(taskInitiationRequestFactory, never()).create(delegateTask);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void should_not_publish_for_another_process_definition() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(delegateTask.getTaskDefinitionKey()).thenReturn("processTask");
        when(delegateTask.getProcessDefinitionId()).thenReturn("process-definition-id");
        when(repositoryService.getProcessDefinition("process-definition-id")).thenReturn(processDefinition);
        when(processDefinition.getKey()).thenReturn("other-process");

        taskInitiationRequestPublisher.publishTaskInitiationRequest(delegateTask);

        verify(taskInitiationRequestFactory, never()).create(delegateTask);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }
}
