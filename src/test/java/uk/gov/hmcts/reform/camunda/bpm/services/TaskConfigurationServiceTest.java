package uk.gov.hmcts.reform.camunda.bpm.services;

import feign.FeignException;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.ConfigureTaskRequest;
import uk.gov.hmcts.reform.camunda.bpm.domain.response.ConfigureTaskResponse;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskConfigurationServiceTest {


    private static final int MAX_RETRIES = 3;
    private static final String TASK_ID = "reviewTheAppeal";
    private static final String CASE_ID = "CASE_123456789";
    private static final String TASK_NAME = "A task name";
    private static final String SERVICE_TOKEN = "Bearer SERVICE_TOKEN";
    private TaskConfigurationService taskConfigurationService;
    @Mock
    private TaskConfigurationServiceApi taskConfigurationServiceApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Spy
    private DelegateTask testTask;
    private String taskId;

    @Before
    public void setUp() {
        taskConfigurationService = new TaskConfigurationService(
            MAX_RETRIES,
            authTokenGenerator,
            taskConfigurationServiceApi,
            "task-configuration"
        );

        taskId = UUID.randomUUID().toString();

        when(authTokenGenerator.generate()).thenReturn(SERVICE_TOKEN);
        when(testTask.getId()).thenReturn(taskId);
        when(testTask.getVariables())
            .thenReturn(getRequiredVariables());

    }

    @Test
    public void should_throw_exception_if_id_is_null() {

        when(testTask.getId()).thenReturn(null);
        assertThatThrownBy(() -> taskConfigurationService.configureTask(testTask))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("taskId cannot be null")
            .hasNoCause();
    }

    @Test
    public void should_successfully_update_the_mutable_object_with_new_values_and_no_assignee() {

        Map<String, Object> responseProcessVariables =
            Map.of(
                "caseTypeId", "Asylum",
                "taskState", "unassigned",
                "executionType", "Case Management Task",
                "caseId", CASE_ID,
                "securityClassification", "PUBLIC",
                "autoAssigned", false,
                "taskSystem", "SELF",
                "name", TASK_NAME,
                "taskType", "reviewTheAppeal"
            );

        when(taskConfigurationServiceApi.configureTask(
            eq(SERVICE_TOKEN),
            eq("task-configuration"),
            eq(taskId),
            any(ConfigureTaskRequest.class)
        )).thenReturn(
            new ConfigureTaskResponse(
                taskId,
                CASE_ID,
                null,
                responseProcessVariables
            )
        );

        taskConfigurationService.configureTask(testTask);

        Map<String, Object> expectedVariables =
            Map.of(
                "caseTypeId", "Asylum",
                "taskState", "unassigned",
                "executionType", "Case Management Task",
                "caseId", CASE_ID,
                "securityClassification", "PUBLIC",
                "autoAssigned", false,
                "taskSystem", "SELF",
                "name", TASK_NAME,
                "taskType", "reviewTheAppeal"
            );

        verify(testTask, times(0)).setAssignee(any());
        verify(testTask, times(1)).setVariablesLocal(expectedVariables);
        verify(testTask, never()).setVariable(any(), any());
        verify(testTask, never()).setVariables(any());

    }

    @Test
    public void should_successfully_update_the_mutable_object_with_new_values_and_auto_assignment() {

        String assignee = UUID.randomUUID().toString();

        Map<String, Object> responseProcessVariables =
            Map.of(
                "caseTypeId", "Asylum",
                "taskState", "assigned",
                "executionType", "Case Management Task",
                "caseId", CASE_ID,
                "securityClassification", "PUBLIC",
                "autoAssigned", true,
                "taskSystem", "SELF",
                "taskType", "reviewTheAppeal",
                "name", TASK_NAME
            );

        when(taskConfigurationServiceApi.configureTask(
                eq(SERVICE_TOKEN),
                eq("task-configuration"),
                eq(taskId),
                any(ConfigureTaskRequest.class)
        )).thenReturn(
            new ConfigureTaskResponse(
                taskId,
                CASE_ID,
                assignee,
                responseProcessVariables
            )
        );

        taskConfigurationService.configureTask(testTask);

        Map<String, Object> expectedVariables =
            Map.of(
                "caseTypeId", "Asylum",
                "taskState", "assigned",
                "executionType", "Case Management Task",
                "caseId", CASE_ID,
                "securityClassification", "PUBLIC",
                "autoAssigned", true,
                "taskSystem", "SELF",
                "name", TASK_NAME,
                "taskType", "reviewTheAppeal"
            );

        verify(testTask, times(1)).setAssignee(assignee);
        verify(testTask, times(1)).setVariablesLocal(expectedVariables);
        verify(testTask, never()).setVariable(any(), any());
        verify(testTask, never()).setVariables(any());

    }

    @Test
    public void should_set_task_state_to_unconfigured_when_task_configuration_feign_exception() {
        when(taskConfigurationServiceApi.configureTask(
                eq(SERVICE_TOKEN),
                eq("task-configuration"),
                eq(taskId),
                any(ConfigureTaskRequest.class)
        )).thenThrow(FeignException.FeignServerException.class);

        taskConfigurationService.configureTask(testTask);

        verify(testTask, times(1)).setVariableLocal("taskState", "unconfigured");
        verify(testTask, never()).setVariable(any(), any());
        verify(testTask, never()).setVariables(any());

    }

    private Map<String, Object> getRequiredVariables() {
        return
            Map.of(
                "taskId", TASK_ID,
                "caseId", CASE_ID,
                "name", TASK_NAME
            );

    }

}
