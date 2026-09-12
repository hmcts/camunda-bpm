package uk.gov.hmcts.reform.camunda.bpm.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.camunda.bpm.config.FeignClientSnakeCaseConfiguration;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.TaskInitiationRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.camunda.bpm.config.ServiceTokenGeneratorConfiguration.SERVICE_AUTHORIZATION;

@FeignClient(
    name = "task-management-initiation-api",
    url = "${task-management-api.url}",
    configuration = FeignClientSnakeCaseConfiguration.class
)
public interface TaskInitiationServiceApi {

    @PostMapping(
        value = "/task/{task-id}/initiation-push",
        consumes = APPLICATION_JSON_VALUE
    )
    void pushInitiation(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                        @PathVariable("task-id") String taskId,
                        @RequestBody TaskInitiationRequest body);
}
