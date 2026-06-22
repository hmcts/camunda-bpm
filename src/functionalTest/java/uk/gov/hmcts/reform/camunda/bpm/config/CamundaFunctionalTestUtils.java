package uk.gov.hmcts.reform.camunda.bpm.config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.MultiPartSpecification;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

class CamundaFunctionalTestUtils {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String TASK_NAME = "Process Application";
    private static final String TASK_TYPE = "processApplication";
    private static final String CASE_TYPE_ID = "WaCaseType";
    private static final String JURISDICTION = "WA";
    private static final String LOCATION = "765324";
    private static final String LOCATION_NAME = "Taylor House";
    private static final String CASE_CATEGORY = "Protection";
    private static final String TASK_DESCRIPTION = "Process application for functional test";
    private static final String AUTHORIZATION = "Authorization";
    private static final String CREATE_EVENT_ID = "CREATE";
    private static final String START_PROGRESS_EVENT_ID = "START_PROGRESS";
    private static final String EVENT_SUMMARY = "summary";
    private static final String EVENT_DESCRIPTION = "description";
    private static final String CLEANUP_TERMINATE_REASON = "Functional test cleanup";

    private final String camundaUrl;
    private final String taskManagementUrl;
    private final String ccdUrl;
    private final String idamUrl;
    private final String s2sUrl;
    private final String s2sName;
    private final String s2sSecret;
    private final String ccdS2sName;
    private final String ccdS2sSecret;
    private final String idamRedirectUrl;
    private final String idamScope;
    private final String idamClientId;
    private final String idamClientSecret;
    private final String waSystemUsername;
    private final String waSystemPassword;

    private String camundaServiceToken;
    private String deploymentId;

    CamundaFunctionalTestUtils(
        @Value("${targets.instance}") String camundaUrl,
        @Value("${targets.task-management}") String taskManagementUrl,
        @Value("${core_case_data.api.url}") String ccdUrl,
        @Value("${idam.api.baseUrl}") String idamUrl,
        @Value("${idam.s2s-auth.url}") String s2sUrl,
        @Value("${idam.s2s-auth.name}") String s2sName,
        @Value("${idam.s2s-auth.secret}") String s2sSecret,
        @Value("${idam.s2s-auth.ccd-name}") String ccdS2sName,
        @Value("${idam.s2s-auth.ccd-secret}") String ccdS2sSecret,
        @Value("${idam.redirectUrl}") String idamRedirectUrl,
        @Value("${idam.scope}") String idamScope,
        @Value("${spring.security.oauth2.client.registration.oidc.client-id}") String idamClientId,
        @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") String idamClientSecret,
        @Value("${idam.system.username}") String waSystemUsername,
        @Value("${idam.system.password}") String waSystemPassword
    ) {
        this.camundaUrl = camundaUrl;
        this.taskManagementUrl = taskManagementUrl;
        this.ccdUrl = ccdUrl;
        this.idamUrl = idamUrl;
        this.s2sUrl = s2sUrl;
        this.s2sName = s2sName;
        this.s2sSecret = s2sSecret;
        this.ccdS2sName = ccdS2sName;
        this.ccdS2sSecret = ccdS2sSecret;
        this.idamRedirectUrl = idamRedirectUrl;
        this.idamScope = idamScope;
        this.idamClientId = idamClientId;
        this.idamClientSecret = idamClientSecret;
        this.waSystemUsername = waSystemUsername;
        this.waSystemPassword = waSystemPassword;
    }

    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        deploymentId = null;
        camundaServiceToken = camundaServiceToken();
    }

    void cleanUp(String taskId) {
        if (camundaServiceToken == null) {
            return;
        }

        terminateTask(taskId);
        deleteDeployment();
    }

    ProcessDefinition deployTaskProcess() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String processId = "waTaskInitiationPushFunctionalTest" + suffix;
        String messageName = "createTaskMessage" + suffix;
        MultiPartSpecification bpmn = new MultiPartSpecBuilder(bpmn(processId, messageName).getBytes(
            StandardCharsets.UTF_8
        ))
            .controlName("data")
            .fileName(processId + ".bpmn")
            .mimeType("text/xml")
            .build();

        Response response = given()
            .header(SERVICE_AUTHORIZATION, camundaServiceToken)
            .baseUri(camundaUrl)
            .multiPart("deployment-name", processId)
            .multiPart(bpmn)
            .when()
            .post("/engine-rest/deployment/create");
        assertThat(response.statusCode()).as(response.asString()).isEqualTo(200);

        deploymentId = response.path("id");
        return new ProcessDefinition(processId, messageName);
    }

    void correlateCreateTaskMessage(ProcessDefinition processDefinition, String caseId) {
        given()
            .header(SERVICE_AUTHORIZATION, camundaServiceToken)
            .baseUri(camundaUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .body(Map.of(
                "messageName", processDefinition.messageName(),
                "processDefinitionKey", processDefinition.processId(),
                "processVariables", taskProcessVariables(caseId)
            ))
            .when()
            .post("/engine-rest/message")
            .then()
            .statusCode(204);
    }

    String getCreatedTaskId(String processId) {
        return given()
            .header(SERVICE_AUTHORIZATION, camundaServiceToken)
            .baseUri(camundaUrl)
            .accept(APPLICATION_JSON_VALUE)
            .queryParam("processDefinitionKey", processId)
            .when()
            .get("/engine-rest/task")
            .then()
            .statusCode(200)
            .body("size()", is(1))
            .extract()
            .path("[0].id");
    }

    String cftTaskState(String taskId) {
        return given()
            .header(SERVICE_AUTHORIZATION, camundaServiceToken)
            .accept(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .when()
            .get("/engine-rest/task/{taskId}/localVariables/cftTaskState", taskId)
            .then()
            .statusCode(200)
            .extract()
            .path("value");
    }

    String createWaCcdCase() {
        assertThat(waSystemUsername)
            .as("WA_SYSTEM_USERNAME must be set so the functional test can create a CCD case")
            .isNotBlank();

        String userToken = waSystemUserToken();
        String ccdServiceToken = ccdServiceToken();
        String userId = userId(userToken);

        Response createStart = startCreateCase(userToken, ccdServiceToken, userId);
        String caseId = submitCreateCase(
            userToken,
            ccdServiceToken,
            userId,
            createStart.path("event_id"),
            createStart.path("token"),
            waCaseData()
        );

        moveCaseToStartProgress(userToken, ccdServiceToken, userId, caseId);
        return caseId;
    }

    private Map<String, Object> taskProcessVariables(String caseId) {
        return Map.ofEntries(
            entry("caseId", variable(caseId)),
            entry("caseTypeId", variable(CASE_TYPE_ID)),
            entry("jurisdiction", variable(JURISDICTION)),
            entry("region", variable("1")),
            entry("location", variable(LOCATION)),
            entry("locationName", variable(LOCATION_NAME)),
            entry("staffLocation", variable(LOCATION_NAME)),
            entry("securityClassification", variable("PUBLIC")),
            entry("name", variable(TASK_NAME)),
            entry("taskId", variable(TASK_TYPE)),
            entry("taskType", variable(TASK_TYPE)),
            entry("taskCategory", variable("Case Progression")),
            entry("taskState", variable("unconfigured")),
            entry("taskSystem", variable("SELF")),
            entry("title", variable(TASK_NAME)),
            entry("executionType", variable("Case Management Task")),
            entry("caseName", variable("Functional Camunda Push")),
            entry("caseCategory", variable(CASE_CATEGORY)),
            entry("caseManagementCategory", variable(CASE_CATEGORY)),
            entry("workType", variable("hearing_work")),
            entry("roleCategory", variable("LEGAL_OPERATIONS")),
            entry("task-supervisor", variable("Read,Refer,Manage,Cancel")),
            entry("tribunal-caseworker", variable("Read,Refer,Own,Manage,Cancel")),
            entry("senior-tribunal-caseworker", variable("Read,Refer,Own,Manage,Cancel")),
            entry("delayUntil", variable("2022-12-01T00:00:00.000+0000")),
            entry("workingDaysAllowed", variable("2")),
            entry("warningList", variable("[]")),
            entry("__processCategory__Protection", booleanVariable(true)),
            entry("hasWarnings", booleanVariable(false))
        );
    }

    private Response startCreateCase(String userToken, String ccdServiceToken, String userId) {
        return given()
            .header(AUTHORIZATION, userToken)
            .header(SERVICE_AUTHORIZATION, ccdServiceToken)
            .contentType(APPLICATION_JSON_VALUE)
            .accept(APPLICATION_JSON_VALUE)
            .baseUri(ccdUrl)
            .when()
            .get(
                "/caseworkers/{userId}/jurisdictions/{jurisdiction}/case-types/{caseType}/event-triggers/{eventId}"
                    + "/token",
                userId,
                JURISDICTION,
                CASE_TYPE_ID,
                CREATE_EVENT_ID
            )
            .then()
            .statusCode(200)
            .extract()
            .response();
    }

    private void moveCaseToStartProgress(String userToken, String ccdServiceToken, String userId, String caseId) {
        Response startProgress = startExistingCaseEvent(
            userToken,
            ccdServiceToken,
            userId,
            caseId,
            START_PROGRESS_EVENT_ID
        );

        submitExistingCaseEvent(
            userToken,
            ccdServiceToken,
            userId,
            caseId,
            startProgress.path("event_id"),
            startProgress.path("token"),
            caseDataFromStartEvent(startProgress)
        );
    }

    private Response startExistingCaseEvent(String userToken,
                                            String ccdServiceToken,
                                            String userId,
                                            String caseId,
                                            String eventId) {
        return given()
            .header(AUTHORIZATION, userToken)
            .header(SERVICE_AUTHORIZATION, ccdServiceToken)
            .contentType(APPLICATION_JSON_VALUE)
            .accept(APPLICATION_JSON_VALUE)
            .baseUri(ccdUrl)
            .when()
            .get(
                "/caseworkers/{userId}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseId}"
                    + "/event-triggers/{eventId}/token",
                userId,
                JURISDICTION,
                CASE_TYPE_ID,
                caseId,
                eventId
            )
            .then()
            .statusCode(200)
            .extract()
            .response();
    }

    private String submitCreateCase(String userToken,
                                    String ccdServiceToken,
                                    String userId,
                                    String eventId,
                                    String eventToken,
                                    Map<String, Object> data) {
        Object caseId = given()
            .header(AUTHORIZATION, userToken)
            .header(SERVICE_AUTHORIZATION, ccdServiceToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(ccdUrl)
            .queryParam("ignore-warning", true)
            .body(caseDataContent(eventId, eventToken, data))
            .when()
            .post(
                "/caseworkers/{userId}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases",
                userId,
                JURISDICTION,
                CASE_TYPE_ID
            )
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        return String.valueOf(caseId);
    }

    private void submitExistingCaseEvent(String userToken,
                                         String ccdServiceToken,
                                         String userId,
                                         String caseId,
                                         String eventId,
                                         String eventToken,
                                         Map<String, Object> data) {
        given()
            .header(AUTHORIZATION, userToken)
            .header(SERVICE_AUTHORIZATION, ccdServiceToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(ccdUrl)
            .queryParam("ignore-warning", true)
            .body(caseDataContent(eventId, eventToken, data))
            .when()
            .post(
                "/caseworkers/{userId}/jurisdictions/{jurisdiction}/case-types/{caseType}/cases/{caseId}/events",
                userId,
                JURISDICTION,
                CASE_TYPE_ID,
                caseId
            )
            .then()
            .statusCode(201);
    }

    private Map<String, Object> caseDataContent(String eventId, String eventToken, Map<String, Object> data) {
        return Map.of(
            "event_token", eventToken,
            "event", caseEvent(eventId),
            "data", data
        );
    }

    private Map<String, Object> caseEvent(String eventId) {
        return Map.of(
            "id", eventId,
            "summary", EVENT_SUMMARY,
            "description", EVENT_DESCRIPTION
        );
    }

    private Map<String, Object> caseDataFromStartEvent(Response response) {
        Map<String, Object> data = response.path("case_details.case_data");
        assertThat(data)
            .as("Expected CCD start event response to contain case_details.case_data: %s", response.asString())
            .isNotNull();
        return data;
    }

    private Map<String, Object> waCaseData() {
        return Map.ofEntries(
            entry("TextField", "hi world"),
            entry("appellantGivenNames", "Functional"),
            entry("appellantFamilyName", "Camunda"),
            entry("caseAccessCategory", "categoryA,categoryC"),
            entry("appealType", "protection"),
            entry("nextHearingId", "next-hearing-id"),
            entry("nextHearingDate", "2022-12-07T14:00:00+01:00"),
            entry("caseManagementLocation", Map.of("baseLocation", LOCATION)),
            entry("staffLocation", LOCATION_NAME)
        );
    }

    private void terminateTask(String taskId) {
        if (taskId == null) {
            return;
        }

        try {
            given()
                .header(SERVICE_AUTHORIZATION, camundaServiceToken)
                .baseUri(taskManagementUrl)
                .contentType(APPLICATION_JSON_VALUE)
                .body(Map.of("terminate_info", Map.of("terminate_reason", CLEANUP_TERMINATE_REASON)))
                .when()
                .delete("/task/{taskId}", taskId);
        } catch (Exception ignored) {
            // Best-effort cleanup only. The deployment cleanup below removes the Camunda side.
        }
    }

    private void deleteDeployment() {
        if (deploymentId == null) {
            return;
        }

        given()
            .header(SERVICE_AUTHORIZATION, camundaServiceToken)
            .baseUri(camundaUrl)
            .queryParam("cascade", true)
            .when()
            .delete("/engine-rest/deployment/{deploymentId}", deploymentId)
            .then()
            .statusCode(204);
        deploymentId = null;
    }

    private String camundaServiceToken() {
        return serviceToken(s2sName, s2sSecret);
    }

    private String serviceToken(String microservice, String secret) {
        String oneTimePassword = format("%06d", new GoogleAuthenticator().getTotpPassword(secret));

        Response response = given()
            .baseUri(s2sUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .accept(TEXT_PLAIN_VALUE)
            .body(Map.of(
                "microservice", microservice,
                "oneTimePassword", oneTimePassword
            ))
            .when()
            .post("/lease");

        if (response.statusCode() == 200) {
            return response.asString();
        }

        return given()
            .baseUri(s2sUrl)
            .contentType(APPLICATION_JSON_VALUE)
            .accept(TEXT_PLAIN_VALUE)
            .body(Map.of("microservice", microservice))
            .when()
            .post("/testing-support/lease")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    }

    private String ccdServiceToken() {
        return serviceToken(ccdS2sName, ccdS2sSecret);
    }

    private String waSystemUserToken() {
        return "Bearer " + given()
            .baseUri(idamUrl)
            .contentType(APPLICATION_FORM_URLENCODED_VALUE)
            .formParam("grant_type", "password")
            .formParam("redirect_uri", idamRedirectUrl)
            .formParam("client_id", idamClientId)
            .formParam("client_secret", idamClientSecret)
            .formParam("username", waSystemUsername)
            .formParam("password", waSystemPassword)
            .formParam("scope", idamScope)
            .when()
            .post("/o/token")
            .then()
            .statusCode(200)
            .extract()
            .path("access_token");
    }

    private String userId(String userToken) {
        return given()
            .baseUri(idamUrl)
            .header(AUTHORIZATION, userToken)
            .accept(APPLICATION_JSON_VALUE)
            .when()
            .get("/o/userinfo")
            .then()
            .statusCode(200)
            .extract()
            .path("uid");
    }

    private Map<String, Object> variable(String value) {
        return Map.of(
            "value", value,
            "type", "String"
        );
    }

    private Map<String, Object> booleanVariable(boolean value) {
        return Map.of(
            "value", value,
            "type", "Boolean"
        );
    }

    private String bpmn(String processId, String messageName) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                              xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                              id="Definitions_task_push_functional_test"
                              targetNamespace="http://bpmn.io/schema/bpmn">
              <bpmn:message id="Message_%2$s" name="%2$s" />
              <bpmn:process id="%1$s"
                            name="Create User Task"
                            isExecutable="true"
                            camunda:historyTimeToLive="P90D">
                <bpmn:startEvent id="createTaskMessage" name="Create User task message received">
                  <bpmn:outgoing>Flow_start_to_process_task</bpmn:outgoing>
                  <bpmn:messageEventDefinition id="MessageEventDefinition_create_task"
                                               messageRef="Message_%2$s" />
                </bpmn:startEvent>
                <bpmn:userTask id="processTask"
                               name="${name}"
                               camunda:dueDate="P10D">
                  <bpmn:documentation>%3$s</bpmn:documentation>
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
            """.formatted(processId, messageName, TASK_DESCRIPTION);
    }

    record ProcessDefinition(String processId, String messageName) {
    }
}
