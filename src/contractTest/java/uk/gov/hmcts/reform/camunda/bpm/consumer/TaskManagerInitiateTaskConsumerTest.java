package uk.gov.hmcts.reform.camunda.bpm.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.util.Map;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.camunda.bpm.config.ServiceTokenGeneratorConfiguration.SERVICE_AUTHORIZATION;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TaskManagementConsumerApplication.class)
@TestPropertySource(properties = "task-management-api.url=http://localhost:8991")
@PactTestFor(providerName = "wa_task_management_api_initiate_task_by_id", port = "8991")
class TaskManagerInitiateTaskConsumerTest {

    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String SERVICE_AUTH_TOKEN = "Bearer someServiceAuthorizationToken";
    private static final String INITIATION_PATH = "/task/" + TASK_ID + "/initiation";

    @Autowired
    private TaskConfigurationServiceApi taskConfigurationServiceApi;

    @Pact(provider = "wa_task_management_api_initiate_task_by_id", consumer = "camunda_bpm")
    RequestResponsePact initiateTaskById(PactDslWithProvider builder) {
        PactDslJsonBody requestBody = (PactDslJsonBody) new PactDslJsonBody()
            .stringValue("operation", "INITIATION")
            .object("task_attributes")
                .stringValue("name", "Process Application")
                .stringValue("taskType", "processApplication")
                .stringValue("caseId", "1234567890123456")
            .closeObject();

        return builder
            .given("initiate a task using taskId")
            .uponReceiving("a request to initiate a task")
            .path(INITIATION_PATH)
            .method("POST")
            .matchHeader(CONTENT_TYPE, "application/json.*", APPLICATION_JSON_VALUE)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(requestBody)
            .willRespondWith()
            .status(CREATED.value())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "initiateTaskById", pactVersion = PactSpecVersion.V3)
    void should_initiate_task_when_request_is_valid(MockServer mockServer) {
        InitiateTaskRequest request = new InitiateTaskRequest(
            "INITIATION",
            Map.of(
                "name", "Process Application",
                "taskType", "processApplication",
                "caseId", "1234567890123456"
            )
        );

        taskConfigurationServiceApi.initiateTask(SERVICE_AUTH_TOKEN, TASK_ID, request);
    }
}
