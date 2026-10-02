package uk.gov.hmcts.reform.camunda.bpm.domain.event;

import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

public record TaskInitiationRequestedEvent(String taskId, InitiateTaskRequest request) {
}
