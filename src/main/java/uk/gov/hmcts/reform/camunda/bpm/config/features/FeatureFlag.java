package uk.gov.hmcts.reform.camunda.bpm.config.features;

public enum FeatureFlag {

    WA_INITIATE_TASKS_ON_CREATE("wa-initiate-tasks-on-create");

    private final String key;

    FeatureFlag(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
