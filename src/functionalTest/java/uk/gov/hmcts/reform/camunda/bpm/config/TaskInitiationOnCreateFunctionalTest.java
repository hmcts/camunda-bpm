package uk.gov.hmcts.reform.camunda.bpm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {
    CamundaFunctionalTestUtils.class,
    FunctionalTestServiceAuthConfiguration.class
})
@ActiveProfiles("functional")
@Disabled("Disabled until WA_INITIATE_TASKS_ON_CREATE LaunchDarkly flag is enabled")
class TaskInitiationOnCreateFunctionalTest {

    private static final Logger LOG = LoggerFactory.getLogger(TaskInitiationOnCreateFunctionalTest.class);
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
        LOG.info("Cleaning up task initiation functional test resources for task id: {}", taskId);
        testUtils.cleanUp(taskId);
        LOG.info("Task initiation functional test cleanup completed");
    }

    @Test
    void should_initiate_task_when_task_committed_successfully() {
        LOG.info("Starting successful task initiation functional test");
        String caseId = testUtils.createWaCcdCase();
        CamundaFunctionalTestUtils.ProcessDefinition processDefinition = testUtils.deployTaskProcess();
        LOG.info("Created CCD case: {} and deployed Camunda process: {}", caseId, processDefinition.processId());

        testUtils.correlateCreateTaskMessage(processDefinition, caseId);
        taskId = testUtils.getCreatedTaskId(processDefinition.processId());
        LOG.info("Created Camunda task: {}. Waiting for cftTaskState to become unassigned", taskId);

        await()
            .ignoreExceptions()
            .pollInterval(2, SECONDS)
            .atMost(60, SECONDS)
            .untilAsserted(() -> assertThat(testUtils.cftTaskState(taskId))
                .isEqualTo("unassigned"));
        LOG.info("Task: {} was initiated successfully and is now unassigned", taskId);
    }

    @Test
    void should_set_task_to_unconfigured_when_camunda_initiation_fails() {
        LOG.info("Starting failed task initiation functional test using missing CCD case: {}", MISSING_CCD_CASE_ID);
        CamundaFunctionalTestUtils.ProcessDefinition processDefinition = testUtils.deployTaskProcess();
        LOG.info("Deployed Camunda process: {}", processDefinition.processId());

        testUtils.correlateCreateTaskMessage(processDefinition, MISSING_CCD_CASE_ID);
        taskId = testUtils.getCreatedTaskId(processDefinition.processId());
        LOG.info("Created Camunda task: {}. Waiting for cftTaskState to remain unconfigured", taskId);

        await()
            .ignoreExceptions()
            .pollInterval(2, SECONDS)
            .atMost(60, SECONDS)
            .untilAsserted(() -> assertThat(testUtils.cftTaskState(taskId)).isEqualTo("unconfigured"));
        LOG.info("Task: {} remained unconfigured after initiation failed as expected", taskId);
    }
}
