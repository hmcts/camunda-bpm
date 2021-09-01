package uk.gov.hmcts.reform.camunda.bpm.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.camunda.bpm.config.FeignClientSnakeCaseConfiguration;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.ConfigureTaskRequest;
import uk.gov.hmcts.reform.camunda.bpm.domain.response.ConfigureTaskResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.camunda.bpm.config.ServiceTokenGeneratorConfiguration.SERVICE_AUTHORIZATION;

@FeignClient(
    name = "task-configuration-api",
    url = "${task-configuration-api.url}",
    configuration = FeignClientSnakeCaseConfiguration.class
)
@SuppressWarnings("checkstyle:LineLength")
public interface TaskConfigurationServiceApi {

    @PostMapping(
        value = "/task-configuration/{task-id}/configuration",
        consumes = APPLICATION_JSON_VALUE
    )
    ConfigureTaskResponse configureTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                        @PathVariable("task-id") String taskId,
                                        @RequestBody ConfigureTaskRequest body);
}
