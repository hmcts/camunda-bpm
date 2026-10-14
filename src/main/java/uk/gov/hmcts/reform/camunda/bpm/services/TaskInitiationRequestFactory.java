package uk.gov.hmcts.reform.camunda.bpm.services;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.camunda.bpm.domain.request.InitiateTaskRequest;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class TaskInitiationRequestFactory {

    private static final String INITIATION_OPERATION = "INITIATION";
    private static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public InitiateTaskRequest create(DelegateTask delegateTask) {
        return new InitiateTaskRequest(
            INITIATION_OPERATION,
            taskAttributes(delegateTask)
        );
    }

    private Map<String, Object> taskAttributes(DelegateTask delegateTask) {
        Map<String, Object> variables = delegateTask.getVariables();
        Map<String, Object> attributes = variables == null ? new HashMap<>() : new HashMap<>(variables);

        attributes.remove("assignee");
        attributes.remove("description");
        attributes.remove("dueDate");
        attributes.remove("name");
        attributes.remove("priorityDate");

        Object taskType = attributes.get("taskType") != null ? attributes.get("taskType") : attributes.get("taskId");
        attributes.put("taskType", taskType);
        attributes.put("name", delegateTask.getName());

        putIfNotNull(attributes, "dueDate", format(delegateTask.getDueDate()));
        putIfNotNull(attributes, "created", format(delegateTask.getCreateTime()));
        putIfNotNull(attributes, "assignee", delegateTask.getAssignee());
        putIfNotNull(attributes, "description", delegateTask.getDescription());

        return attributes;
    }

    private void putIfNotNull(Map<String, Object> attributes, String name, Object value) {
        if (value != null) {
            attributes.put(name, value);
        }
    }

    private String format(Date date) {
        if (date == null) {
            return null;
        }
        return CAMUNDA_DATA_TIME_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }
}
