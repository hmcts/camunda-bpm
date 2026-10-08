package uk.gov.hmcts.reform.camunda.bpm.domain.request;

public class CamundaVariable {

    private final Object value;
    private final String type;

    public CamundaVariable(Object value, String type) {
        this.value = value;
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
