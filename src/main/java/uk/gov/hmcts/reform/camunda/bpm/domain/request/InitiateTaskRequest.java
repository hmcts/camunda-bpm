package uk.gov.hmcts.reform.camunda.bpm.domain.request;

import java.util.Map;

public class InitiateTaskRequest {

    private final String operation;
    private final Map<String, Object> taskAttributes;

    public InitiateTaskRequest(String operation, Map<String, Object> taskAttributes) {
        this.operation = operation;
        this.taskAttributes = taskAttributes;
    }

    public String getOperation() {
        return operation;
    }

    public Map<String, Object> getTaskAttributes() {
        return taskAttributes;
    }
}
