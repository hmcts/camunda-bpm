package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

@Component
public class TaskInitiationRequestPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskInitiationRequestFactory taskInitiationRequestFactory;

    public TaskInitiationRequestPublisher(ApplicationEventPublisher applicationEventPublisher,
                                          TaskInitiationRequestFactory taskInitiationRequestFactory) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.taskInitiationRequestFactory = taskInitiationRequestFactory;
    }

    public void publishTaskInitiationRequest(DelegateTask delegateTask) {
        String taskId = delegateTask.getId();
        InitiateTaskRequest request = taskInitiationRequestFactory.create(delegateTask);

        applicationEventPublisher.publishEvent(new TaskInitiationRequestedEvent(taskId, request));
    }
}
