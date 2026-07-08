package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskInitiationRequestFactoryTest {

    private TaskInitiationRequestFactory taskInitiationRequestFactory;

    @Before
    public void setUp() {
        taskInitiationRequestFactory = new TaskInitiationRequestFactory();
    }

    @Test
    public void should_create_task_initiation_request_with_task_attributes() {
        DelegateTask delegateTask = delegateTask();

        InitiateTaskRequest request = taskInitiationRequestFactory.create(delegateTask);

        assertThat(request.getOperation()).isEqualTo("INITIATION");
        assertThat(request.getTaskAttributes())
            .containsEntry("caseId", "1678901234567890")
            .containsEntry("caseTypeId", "WaCaseType")
            .containsEntry("jurisdiction", "WA")
            .containsEntry("name", "Process Application")
            .containsEntry("taskType", "processApplication");
    }

    private DelegateTask delegateTask() {
        DelegateTask delegateTask = mock(DelegateTask.class);
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
