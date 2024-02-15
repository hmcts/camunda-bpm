package uk.gov.hmcts.reform.camunda.bpm.config;

import java.util.List;

public class GroupConfig {

    private String adGroupId;
    private String tenantId;
    private String groupId;
    private String accessControl;
    private List<String> s2sServiceNames;

    public String getAdGroupId() {
        return adGroupId;
    }

    public void setAdGroupId(String adGroupId) {
        this.adGroupId = adGroupId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(String accessControl) {
        this.accessControl = accessControl;
    }

    public List<String> getS2sServiceNames() {
        return s2sServiceNames;
    }

    public void setS2sServiceNames(List<String> s2sServiceNames) {
        this.s2sServiceNames = s2sServiceNames;
    }
}
