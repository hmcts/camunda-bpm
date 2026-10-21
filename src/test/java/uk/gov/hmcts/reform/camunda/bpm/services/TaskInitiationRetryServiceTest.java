package uk.gov.hmcts.reform.camunda.bpm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskInitiationRetryServiceTest {

    @Test
    void should_propagate_service_unavailable_when_response_body_is_malformed() {
        TaskConfigurationServiceApi taskManagementApi = mock(TaskConfigurationServiceApi.class);
        AuthTokenGenerator authTokenGenerator = mock(AuthTokenGenerator.class);
        TaskInitiationRetryService service = new TaskInitiationRetryService(
            taskManagementApi, authTokenGenerator, new ObjectMapper()
        );
        InitiateTaskRequest request = new InitiateTaskRequest(
            "INITIATION", Map.of("taskType", "processApplication")
        );
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(503);
        when(exception.contentUTF8()).thenReturn("{invalid-json");
        when(authTokenGenerator.generate()).thenReturn("s2s-token");
        doThrow(exception).when(taskManagementApi).initiateTask("s2s-token", "task-id", request);

        FeignException thrown = assertThrows(FeignException.class,
            () -> service.initiateTaskWithRetry("task-id", request));

        assertSame(exception, thrown);
        verify(taskManagementApi).initiateTask("s2s-token", "task-id", request);
    }
}
