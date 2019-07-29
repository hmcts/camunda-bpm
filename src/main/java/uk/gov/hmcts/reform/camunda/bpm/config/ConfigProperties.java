package uk.gov.hmcts.reform.camunda.bpm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties
public class ConfigProperties {

    private Map<String, GroupConfig> camundaGroups;

    private Map<String, AccessControl> accessControl;

    private String camundaAdminGroupId;

    public Map<String, GroupConfig> getCamundaGroups() {
        return camundaGroups;
    }

    public void setCamundaGroups(Map<String, GroupConfig> camundaGroups) {
        this.camundaGroups = camundaGroups;
    }

    public String getCamundaAdminGroupId() {
        return camundaAdminGroupId;
    }

    public void setCamundaAdminGroupId(String camundaAdminGroupId) {
        this.camundaAdminGroupId = camundaAdminGroupId;
    }

    public Map<String, AccessControl> getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(Map<String, AccessControl> accessControl) {
        this.accessControl = accessControl;
    }
}
