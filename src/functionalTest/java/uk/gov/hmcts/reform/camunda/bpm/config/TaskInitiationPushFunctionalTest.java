package uk.gov.hmcts.reform.camunda.bpm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = CamundaFunctionalTestUtils.class)
@ActiveProfiles("functional")
@Disabled("Disabled until WA_INITIATE_TASKS_ON_CREATE_ENABLED is enabled")
class TaskInitiationPushFunctionalTest {

    private static final String MISSING_CCD_CASE_ID = "1678901234567890";

    @Autowired
    private CamundaFunctionalTestUtils testUtils;

    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = null;
        testUtils.setUp();
    }

    @AfterEach
    void tearDown() {
        testUtils.cleanUp(taskId);
    }

    @Test
    void should_initiate_task_when_task_committed_successfully() {
        String caseId = testUtils.createWaCcdCase();
        CamundaFunctionalTestUtils.ProcessDefinition processDefinition = testUtils.deployTaskProcess();

        testUtils.correlateCreateTaskMessage(processDefinition, caseId);
        taskId = testUtils.getCreatedTaskId(processDefinition.processId());

        await()
            .ignoreExceptions()
            .pollInterval(2, SECONDS)
            .atMost(60, SECONDS)
            .untilAsserted(() -> assertThat(testUtils.cftTaskState(taskId))
                .isEqualTo("unassigned"));
    }

    @Test
    void should_set_task_to_unconfigured_when_camunda_initiation_fails() {
        CamundaFunctionalTestUtils.ProcessDefinition processDefinition = testUtils.deployTaskProcess();

        testUtils.correlateCreateTaskMessage(processDefinition, MISSING_CCD_CASE_ID);
        taskId = testUtils.getCreatedTaskId(processDefinition.processId());

        await()
            .ignoreExceptions()
            .pollInterval(2, SECONDS)
            .atMost(60, SECONDS)
            .untilAsserted(() -> assertThat(testUtils.cftTaskState(taskId)).isEqualTo("unconfigured"));
    }
}
