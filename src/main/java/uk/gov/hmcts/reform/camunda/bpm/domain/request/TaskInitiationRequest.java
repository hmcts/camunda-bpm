package uk.gov.hmcts.reform.camunda.bpm.domain.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.ZonedDateTime;
import java.util.Map;

public class TaskInitiationRequest {

    private final String name;
    private final String assignee;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final ZonedDateTime created;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final ZonedDateTime due;

    private final String description;
    private final String processInstanceId;
    private final Map<String, CamundaVariable> variables;

    public TaskInitiationRequest(String name,
                                 String assignee,
                                 ZonedDateTime created,
                                 ZonedDateTime due,
                                 String description,
                                 String processInstanceId,
                                 Map<String, CamundaVariable> variables) {
        this.name = name;
        this.assignee = assignee;
        this.created = created;
        this.due = due;
        this.description = description;
        this.processInstanceId = processInstanceId;
        this.variables = variables;
    }

    public String getName() {
        return name;
    }

    public String getAssignee() {
        return assignee;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public ZonedDateTime getDue() {
        return due;
    }

    public String getDescription() {
        return description;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public Map<String, CamundaVariable> getVariables() {
        return variables;
    }
}
