package uk.gov.hmcts.reform.camunda.bpm.domain.request;

import java.util.Map;

public class ConfigureTaskRequest {
    private Map<String, Object> processVariables;

    private ConfigureTaskRequest() {
        //No-op constructor for deserialization
    }

    public ConfigureTaskRequest(Map<String, Object> processVariables) {
        this.processVariables = processVariables;
    }

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

}
