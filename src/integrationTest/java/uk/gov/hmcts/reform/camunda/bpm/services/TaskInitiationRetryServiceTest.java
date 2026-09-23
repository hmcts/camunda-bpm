package uk.gov.hmcts.reform.camunda.bpm.services;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import(TaskInitiationRetryServiceTest.RetryTestConfiguration.class)
class TaskInitiationRetryServiceTest {

    private static final String TASK_ID = "task-id";
    private static final String SERVICE_TOKEN = "s2s-token";

    @TestConfiguration
    @EnableRetry
    @Import(TaskInitiationRetryService.class)
    static class RetryTestConfiguration {

        @Bean
        TaskConfigurationServiceApi taskConfigurationServiceApi() {
            return mock(TaskConfigurationServiceApi.class);
        }

        @Bean
        AuthTokenGenerator authTokenGenerator() {
            return mock(AuthTokenGenerator.class);
        }
    }

    @Autowired
    private TaskConfigurationServiceApi taskManagementApi;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private TaskInitiationRetryService taskInitiationRetryService;

    @BeforeEach
    void setUp() {
        reset(taskManagementApi, authTokenGenerator);
    }

    @Test
    void should_retry_task_initiation_when_feign_exception_occurs() {
        InitiateTaskRequest request = new InitiateTaskRequest(
            "INITIATION",
            Map.of("taskType", "processApplication")
        );
        when(authTokenGenerator.generate()).thenReturn(SERVICE_TOKEN);
        doThrow(feignException())
            .doThrow(feignException())
            .doNothing()
            .when(taskManagementApi)
            .initiateTask(anyString(), eq(TASK_ID), eq(request));

        taskInitiationRetryService.initiateTaskWithRetry(TASK_ID, request);

        verify(taskManagementApi, times(3)).initiateTask(SERVICE_TOKEN, TASK_ID, request);
        verify(authTokenGenerator, times(3)).generate();
    }

    private FeignException feignException() {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "http://localhost/task/task-id/initiation",
            Collections.emptyMap(),
            new byte[0],
            StandardCharsets.UTF_8,
            null
        );
        Response response = Response.builder()
            .status(500)
            .reason("Internal server error")
            .request(request)
            .headers(Collections.emptyMap())
            .build();
        return FeignException.errorStatus("TaskConfigurationServiceApi#initiateTask", response);
    }
}
