package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.DeploymentWithDefinitions;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
    "camunda.bpm.authorization.enabled=false",
    "configuration.initiateTasksOnCreate=true"
})
class EventHandlerConfigurationPushInitiationTest extends SpringBootIntegrationBaseTest {

    private static final String SERVICE_TOKEN = "S2S_TOKEN";
    private static final String CASE_ID = "1678901234567890";
    private static final String CFT_TASK_STATE = "cftTaskState";
    private static final String TASK_NAME = "Process Application";
    private static final String TASK_TYPE = "processApplication";
    private static final String PROCESS_ID = "wa-task-initiation-push-test";
    private static final String TEST_PROCESS_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                          id="Definitions_task_push_test"
                          targetNamespace="http://bpmn.io/schema/bpmn">
          <bpmn:message id="Message_createTaskMessage" name="createTaskMessage" />
          <bpmn:process id="wa-task-initiation-push-test"
                        name="Create User Task"
                        isExecutable="true"
                        camunda:historyTimeToLive="P90D">
            <bpmn:startEvent id="createTaskMessage" name="Create User task message received">
              <bpmn:outgoing>Flow_start_to_process_task</bpmn:outgoing>
              <bpmn:messageEventDefinition id="MessageEventDefinition_create_task"
                                           messageRef="Message_createTaskMessage" />
            </bpmn:startEvent>
            <bpmn:userTask id="processTask"
                           name="${name}"
                           camunda:dueDate="P2D">
              <bpmn:incoming>Flow_start_to_process_task</bpmn:incoming>
              <bpmn:outgoing>Flow_process_task_to_end</bpmn:outgoing>
            </bpmn:userTask>
            <bpmn:endEvent id="userTaskCompleted" name="User task completed">
              <bpmn:incoming>Flow_process_task_to_end</bpmn:incoming>
            </bpmn:endEvent>
            <bpmn:sequenceFlow id="Flow_start_to_process_task"
                               sourceRef="createTaskMessage"
                               targetRef="processTask" />
            <bpmn:sequenceFlow id="Flow_process_task_to_end"
                               sourceRef="processTask"
                               targetRef="userTaskCompleted" />
          </bpmn:process>
        </bpmn:definitions>
        """;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private TaskConfigurationServiceApi taskManagementApi;

    private String deploymentId;

    @BeforeEach
    void setUp() {
        reset(taskManagementApi);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_TOKEN);

        DeploymentWithDefinitions deployment = repositoryService.createDeployment()
            .addString("wa-task-initiation-push-test.bpmn", TEST_PROCESS_BPMN)
            .deployWithResult();
        deploymentId = deployment.getId();
    }

    @AfterEach
    void tearDown() {
        repositoryService.deleteDeployment(deploymentId, true);
    }

    @Test
    void should_push_task_initiation_request_when_camunda_commit_finished() {
        correlateCreateTaskMessage();

        Task task = taskService.createTaskQuery()
            .processDefinitionKey(PROCESS_ID)
            .singleResult();
        ArgumentCaptor<InitiateTaskRequest> requestCaptor = ArgumentCaptor.forClass(InitiateTaskRequest.class);

        verify(taskManagementApi).initiateTask(eq(SERVICE_TOKEN), eq(task.getId()), requestCaptor.capture());

        InitiateTaskRequest request = requestCaptor.getValue();
        assertThat(request.getOperation()).isEqualTo("INITIATION");
        assertThat(request.getTaskAttributes())
            .containsEntry("caseId", CASE_ID)
            .containsEntry("caseTypeId", "WaCaseType")
            .containsEntry("jurisdiction", "WA")
            .containsEntry("name", TASK_NAME)
            .containsEntry("taskType", TASK_TYPE);
    }

    @Test
    void should_set_task_state_to_unassigned_when_push_initiation_succeeds() {
        doAnswer(invocation -> {
            taskService.setVariableLocal(invocation.getArgument(1), CFT_TASK_STATE, "unassigned");
            return null;
        }).when(taskManagementApi).initiateTask(anyString(), anyString(), any(InitiateTaskRequest.class));

        correlateCreateTaskMessage();

        Task task = taskService.createTaskQuery()
            .processDefinitionKey(PROCESS_ID)
            .singleResult();

        verify(taskManagementApi).initiateTask(eq(SERVICE_TOKEN), eq(task.getId()), any(InitiateTaskRequest.class));
        assertThat(taskService.getVariableLocal(task.getId(), CFT_TASK_STATE)).isEqualTo("unassigned");
    }

    @Test
    void should_set_task_state_to_unconfigured_when_feature_toggle_is_enabled_and_push_initiation_fails() {
        doThrow(new RuntimeException("Task Management unavailable"))
            .when(taskManagementApi)
            .initiateTask(anyString(), anyString(), any(InitiateTaskRequest.class));

        correlateCreateTaskMessage();

        Task task = taskService.createTaskQuery()
            .processDefinitionKey(PROCESS_ID)
            .singleResult();

        verify(taskManagementApi).initiateTask(eq(SERVICE_TOKEN), eq(task.getId()), any(InitiateTaskRequest.class));
        assertThat(taskService.getVariableLocal(task.getId(), CFT_TASK_STATE)).isEqualTo("unconfigured");
    }

    private void correlateCreateTaskMessage() {
        runtimeService.createMessageCorrelation("createTaskMessage")
            .setVariables(Map.of(
                "caseId", CASE_ID,
                "caseTypeId", "WaCaseType",
                "jurisdiction", "WA",
                "name", TASK_NAME,
                "taskId", TASK_TYPE,
                "taskType", TASK_TYPE
            ))
            .correlateStartMessage();
    }
}
