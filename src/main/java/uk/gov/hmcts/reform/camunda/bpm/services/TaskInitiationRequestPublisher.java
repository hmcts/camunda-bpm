package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

@Component
public class TaskInitiationRequestPublisher {

    private static final String TASK_DEFINITION_KEY = "processTask";
    private static final String PROCESS_DEFINITION_KEY = "wa-task-initiation-ia-asylum";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskInitiationRequestFactory taskInitiationRequestFactory;
    private final RepositoryService repositoryService;

    public TaskInitiationRequestPublisher(ApplicationEventPublisher applicationEventPublisher,
                                          TaskInitiationRequestFactory taskInitiationRequestFactory,
                                          RepositoryService repositoryService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.taskInitiationRequestFactory = taskInitiationRequestFactory;
        this.repositoryService = repositoryService;
    }

    public void publishTaskInitiationRequest(DelegateTask delegateTask) {
        if (TASK_DEFINITION_KEY.equals(delegateTask.getTaskDefinitionKey())
            && isTargetProcess(delegateTask)) {
            String taskId = delegateTask.getId();
            InitiateTaskRequest request = taskInitiationRequestFactory.create(delegateTask);
            applicationEventPublisher.publishEvent(new TaskInitiationRequestedEvent(taskId, request));
        }
    }

    private boolean isTargetProcess(DelegateTask delegateTask) {
        String processDefinitionKey;
        if (delegateTask.getExecution() instanceof ExecutionEntity execution) {
            processDefinitionKey = execution.getProcessDefinition().getKey();
        } else {
            processDefinitionKey =
                    repositoryService.getProcessDefinition(delegateTask.getProcessDefinitionId()).getKey();
        }
        return PROCESS_DEFINITION_KEY.equals(processDefinitionKey);
    }
}
