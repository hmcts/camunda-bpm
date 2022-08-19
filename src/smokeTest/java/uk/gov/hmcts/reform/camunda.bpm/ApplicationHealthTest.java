package uk.gov.hmcts.reform.camunda.bpm;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
// import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
//import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.yaml")
public class ApplicationHealthTest {

    @Value("${test-url}")
    private String testUrl;

    @Before
    public void before() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void should_return_UP_for_liveness_check() {
        given()
            .contentType(ContentType.JSON)
            .accept(APPLICATION_JSON_VALUE)
            .when()
            .get(testUrl + "/health/liveness")
            .then()
            .statusCode(404)
            .body("status", is("UP"))
            .extract().response();

        // Assertions.assertEquals(404, response.statusCode());
    }

    @Test
    public void should_have_an_up_status_healthCheck() {
        given()
            .accept(APPLICATION_JSON_VALUE)
            .when()
            .get(testUrl + "/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
    }

}
