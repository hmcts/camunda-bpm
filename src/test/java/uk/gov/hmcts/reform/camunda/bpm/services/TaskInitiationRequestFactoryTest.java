package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskInitiationRequestFactoryTest {

    private static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

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
            .containsEntry("taskType", "processApplication")
            .doesNotContainKeys("dueDate", "created", "assignee", "description");
    }

    @Test
    public void should_create_task_initiation_request_when_variables_are_null() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getName()).thenReturn("Process Application");
        when(delegateTask.getVariables()).thenReturn(null);

        InitiateTaskRequest request = taskInitiationRequestFactory.create(delegateTask);

        assertThat(request.getTaskAttributes())
            .containsEntry("name", "Process Application")
            .containsEntry("taskType", null);
    }

    @Test
    public void should_prefer_task_type_and_include_non_null_task_metadata() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        Date dueDate = Date.from(Instant.parse("2026-08-02T09:30:00Z"));
        Date createdDate = Date.from(Instant.parse("2026-07-31T08:15:00Z"));
        when(delegateTask.getName()).thenReturn("Process Application");
        when(delegateTask.getVariables()).thenReturn(Map.of(
            "taskType", "configuredTaskType",
            "taskId", "fallbackTaskType"
        ));
        when(delegateTask.getDueDate()).thenReturn(dueDate);
        when(delegateTask.getCreateTime()).thenReturn(createdDate);
        when(delegateTask.getAssignee()).thenReturn("caseworker-id");
        when(delegateTask.getDescription()).thenReturn("Task description");

        InitiateTaskRequest request = taskInitiationRequestFactory.create(delegateTask);

        assertThat(request.getTaskAttributes())
            .containsEntry("taskType", "configuredTaskType")
            .containsEntry("dueDate", format(dueDate))
            .containsEntry("created", format(createdDate))
            .containsEntry("assignee", "caseworker-id")
            .containsEntry("description", "Task description");
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

    private String format(Date date) {
        return CAMUNDA_DATA_TIME_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }
}
