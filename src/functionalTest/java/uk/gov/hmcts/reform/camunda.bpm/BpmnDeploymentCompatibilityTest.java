package uk.gov.hmcts.reform.camunda.bpm;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.codec.StringDecoder;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.yaml")
public class BpmnDeploymentCompatibilityTest {

    private static final String BPMN_RESOURCE_DIRECTORY = "bpmn/";
    private static final String DEPLOYMENT_NAME_PREFIX = "camunda-bpmn-test-";
    private static final int REQUEST_TIMEOUT_MILLIS = 30_000;
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String SERVICE_NAME = "wa_camunda_pipeline_upload";

    private static final Fixture SIMPLE_FIXTURE = new Fixture(
        "simple-process.bpmn",
        "CAMUNDA_RELEASE_SIMPLE"
    );
    private static final Fixture CIVIL_FIXTURE = new Fixture(
        "civil-judgment-requested-spec.bpmn",
        "JUDGMENT_REQUESTED_SPEC"
    );

    @Value("${test-url}")
    private String testUrl;

    @Value("${S2S_URL:http://localhost:4552}")
    private String s2sUrl;

    @Value("${S2S_SECRET_WA_CAMUNDA_PIPELINE_UPLOAD:}")
    private String s2sSecret;

    @Value("${RUN_CIVIL_BPMN_DEPLOYMENT_TEST:false}")
    private boolean runCivilBpmnDeploymentTest;

    private AuthTokenGenerator authTokenGenerator;

    @Before
    public void before() {
        RestAssured.useRelaxedHTTPSValidation();
        assertFalse("S2S_SECRET_WA_CAMUNDA_PIPELINE_UPLOAD must be set", s2sSecret.isBlank());
        authTokenGenerator = createAuthTokenGenerator();
    }

    @Test
    public void should_deploy_read_and_remove_simple_process() throws IOException {
        deployReadAndRemove(SIMPLE_FIXTURE);
    }

    @Test
    public void should_deploy_read_and_remove_civil_process_in_aat() throws IOException {
        Assume.assumeTrue("Civil BPMN deployment test runs in AAT only", runCivilBpmnDeploymentTest);
        deployReadAndRemove(CIVIL_FIXTURE);
    }

    private void deployReadAndRemove(Fixture fixture) throws IOException {
        String deploymentId = null;
        String deploymentName = DEPLOYMENT_NAME_PREFIX + UUID.randomUUID();

        try {
            Response deployment = request()
                .multiPart("deployment-name", deploymentName)
                .multiPart("data", fixture.fileName, resourceBytes(fixture.fileName), "application/xml")
                .post(testUrl + "/engine-rest/deployment/create");
            assertStatus(fixture, "upload", deployment, 200);

            deploymentId = deployment.path("id");
            assertNotNull(fixture.fileName + " upload did not return a deployment ID", deploymentId);

            Response processDefinitions = request()
                .queryParam("deploymentId", deploymentId)
                .get(testUrl + "/engine-rest/process-definition");
            assertStatus(fixture, "read back", processDefinitions, 200);

            List<?> deployedProcessDefinitions = processDefinitions.jsonPath().getList("$");
            assertEquals(
                fixture.fileName + " should create exactly one process definition",
                1,
                deployedProcessDefinitions.size()
            );
            assertEquals(
                fixture.fileName + " returned the wrong process definition key",
                fixture.processDefinitionKey,
                processDefinitions.jsonPath().getString("[0].key")
            );
        } finally {
            if (deploymentId == null) {
                deploymentId = findDeploymentId(fixture, deploymentName);
            }
            if (deploymentId != null) {
                removeDeployment(fixture, deploymentId);
            }
        }
    }

    private String findDeploymentId(Fixture fixture, String deploymentName) {
        Response deployments = request()
            .queryParam("name", deploymentName)
            .get(testUrl + "/engine-rest/deployment");
        assertStatus(fixture, "find deployment for cleanup", deployments, 200);

        List<?> matchingDeployments = deployments.jsonPath().getList("$");
        assertTrue(
            fixture.fileName + " upload created more than one deployment with the same name",
            matchingDeployments.size() <= 1
        );
        return matchingDeployments.isEmpty() ? null : deployments.jsonPath().getString("[0].id");
    }

    private void removeDeployment(Fixture fixture, String deploymentId) {
        Response deletion = request()
            .queryParam("cascade", true)
            .delete(testUrl + "/engine-rest/deployment/" + deploymentId);
        assertStatus(fixture, "remove", deletion, 204);

        Response deployment = request().get(testUrl + "/engine-rest/deployment/" + deploymentId);
        assertStatus(fixture, "verify deployment cleanup", deployment, 404);

        Response processDefinitions = request()
            .queryParam("deploymentId", deploymentId)
            .get(testUrl + "/engine-rest/process-definition");
        assertStatus(fixture, "verify process definition cleanup", processDefinitions, 200);
        assertTrue(
            fixture.fileName + " deployment still has process definitions after cleanup",
            processDefinitions.jsonPath().getList("$").isEmpty()
        );
    }

    private RequestSpecification request() {
        return RestAssured.given()
            .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", REQUEST_TIMEOUT_MILLIS)
                .setParam("http.socket.timeout", REQUEST_TIMEOUT_MILLIS)))
            .header(SERVICE_AUTHORIZATION, authTokenGenerator.generate());
    }

    private void assertStatus(Fixture fixture, String operation, Response response, int expectedStatus) {
        if (response.statusCode() != expectedStatus) {
            fail(String.format(
                "%s %s failed: expected HTTP %d but got HTTP %d. Response: %s",
                fixture.fileName,
                operation,
                expectedStatus,
                response.statusCode(),
                response.getBody().asString()
            ));
        }
    }

    private byte[] resourceBytes(String fileName) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(BPMN_RESOURCE_DIRECTORY + fileName)) {
            assertNotNull("Missing BPMN fixture " + fileName, input);
            return input.readAllBytes();
        }
    }

    private AuthTokenGenerator createAuthTokenGenerator() {
        HttpMessageConverter<?> jsonConverter = new MappingJackson2HttpMessageConverter(new ObjectMapper());
        ObjectFactory<HttpMessageConverters> converters = () -> new HttpMessageConverters(jsonConverter);
        ServiceAuthorisationApi serviceAuthorisationApi = Feign.builder()
            .contract(new SpringMvcContract())
            .encoder(new SpringEncoder(converters))
            .decoder(new StringDecoder())
            .target(ServiceAuthorisationApi.class, s2sUrl);

        return AuthTokenGeneratorFactory.createDefaultGenerator(
            s2sSecret,
            SERVICE_NAME,
            serviceAuthorisationApi
        );
    }

    private static final class Fixture {
        private final String fileName;
        private final String processDefinitionKey;

        private Fixture(String fileName, String processDefinitionKey) {
            this.fileName = fileName;
            this.processDefinitionKey = processDefinitionKey;
        }
    }
}
