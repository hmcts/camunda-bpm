package uk.gov.hmcts.reform.camunda.bpm.domain.request;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InitiateTaskRequestTest {

    @Test
    void should_create_initiate_task_request() {
        Map<String, Object> taskAttributes = Map.of(
            "caseId", "1678901234567890",
            "taskType", "processApplication"
        );

        InitiateTaskRequest request = new InitiateTaskRequest("INITIATION", taskAttributes);

        assertThat(request.getOperation()).isEqualTo("INITIATION");
        assertThat(request.getTaskAttributes()).isEqualTo(taskAttributes);
    }
}
