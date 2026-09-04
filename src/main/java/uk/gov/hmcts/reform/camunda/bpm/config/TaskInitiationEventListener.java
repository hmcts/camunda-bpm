package uk.gov.hmcts.reform.camunda.bpm.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.gov.hmcts.reform.camunda.bpm.domain.event.TaskInitiationRequestedEvent;
import uk.gov.hmcts.reform.camunda.bpm.services.TaskInitiationService;

@Component
class TaskInitiationEventListener {

    private final TaskInitiationService taskInitiationService;

    TaskInitiationEventListener(TaskInitiationService taskInitiationService) {
        this.taskInitiationService = taskInitiationService;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void onTaskInitiationRequested(TaskInitiationRequestedEvent event) {
        taskInitiationService.initiateTask(event);
    }
}
