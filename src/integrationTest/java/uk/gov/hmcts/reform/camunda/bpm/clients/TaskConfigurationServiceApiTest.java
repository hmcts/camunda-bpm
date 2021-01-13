package uk.gov.hmcts.reform.camunda.bpm.clients;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.camunda.bpm.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.ConfigureTaskRequest;
import uk.gov.hmcts.reform.camunda.bpm.domain.response.ConfigureTaskResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class TaskConfigurationServiceApiTest extends SpringBootIntegrationBaseTest {

    private static final String CASE_ID = "CASE_12345";
    private static final String TASK_NAME = "A task name";
    private static final String USER_ID = "someUserId";
    private static final String SERVICE_TOKEN = "S2S_TOKEN";
    private static WireMockServer wireMockServer;
    private String taskId;


    @Autowired
    private TaskConfigurationServiceApi taskConfigurationServiceApi;


    @BeforeAll
    static void configure() {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();

    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void setUp() {
        taskId = UUID.randomUUID().toString();
    }

    @Test
    void should_respond_with_correct_variables_with_no_auto_assignment() throws IOException {

        stubTaskConfigurationServiceApiResponse(
            loadJsonResponseFromFile("task-configuration-response-no-auto-assignment.json")
        );

        ConfigureTaskResponse response = taskConfigurationServiceApi.configureTask(
            SERVICE_TOKEN,
            taskId,
            new ConfigureTaskRequest(getRequiredVariables())
        );

        Map<String, Object> expectedConfigurationVariables = getDefaultExpectedVariables();
        expectedConfigurationVariables.put("autoAssigned", false);
        expectedConfigurationVariables.put("taskState", "unassigned");

        assertThat(response).isNotNull();
        assertThat(response.getAssignee()).isNull();
        assertThat(response.getTaskId()).isEqualTo(taskId);
        assertThat(response.getCaseId()).isEqualTo(CASE_ID);
        assertThat(response.getConfigurationVariables()).isEqualTo(expectedConfigurationVariables);
    }

    @Test
    void should_respond_with_correct_variables_with_auto_assignment() throws IOException {

        stubTaskConfigurationServiceApiResponse(
            loadJsonResponseFromFile("task-configuration-response-auto-assignment.json")
        );

        ConfigureTaskResponse response = taskConfigurationServiceApi.configureTask(
            SERVICE_TOKEN,
            taskId,
            new ConfigureTaskRequest(getRequiredVariables())
        );

        Map<String, Object> expectedConfigurationVariables = getDefaultExpectedVariables();
        expectedConfigurationVariables.put("autoAssigned", true);
        expectedConfigurationVariables.put("taskState", "assigned");

        assertThat(response).isNotNull();
        assertThat(response.getAssignee()).isEqualTo(USER_ID);
        assertThat(response.getTaskId()).isEqualTo(taskId);
        assertThat(response.getCaseId()).isEqualTo(CASE_ID);
        assertThat(response.getConfigurationVariables()).isEqualTo(expectedConfigurationVariables);
    }


    private void stubTaskConfigurationServiceApiResponse(String response) {
        String url = "/task/" + taskId + "/configuration";
        wireMockServer.stubFor(
            post(urlEqualTo(url))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(response))
        );
    }

    private String loadJsonResponseFromFile(String fileName) throws IOException {
        String response = FileUtils.readFileToString(
            ResourceUtils.getFile("classpath:responses/" + fileName));

        response = response.replace("{TASK_ID}", taskId)
            .replace("{CASE_ID}", CASE_ID)
            .replace("{TASK_NAME}", TASK_NAME)
            .replace("{USER_ID}", USER_ID);

        return response;

    }

    private Map<String, Object> getRequiredVariables() {
        return
            Map.of(
                "caseId", CASE_ID,
                "name", TASK_NAME
            );

    }


    private Map<String, Object> getDefaultExpectedVariables() {

        Map<String, Object> defaultExpectedVariables = new HashMap<>();
        defaultExpectedVariables.put("appealType", "protection");
        defaultExpectedVariables.put("caseId", CASE_ID);
        defaultExpectedVariables.put("caseName", "Bob Smith");
        defaultExpectedVariables.put("caseTypeId", "Asylum");
        defaultExpectedVariables.put("executionType", "Case Management Task");
        defaultExpectedVariables.put("location", "765324");
        defaultExpectedVariables.put("locationName", "Taylor House");
        defaultExpectedVariables.put("region", "1");
        defaultExpectedVariables.put("securityClassification", "PUBLIC");
        defaultExpectedVariables.put("senior-tribunal-caseworker", "Read,Refer,Own,Manage,Cancel");
        defaultExpectedVariables.put("taskSystem", "SELF");
        defaultExpectedVariables.put("title", TASK_NAME);
        defaultExpectedVariables.put("tribunal-caseworker", "Read,Refer,Own,Manage,Cancel");

        return defaultExpectedVariables;
    }

}
