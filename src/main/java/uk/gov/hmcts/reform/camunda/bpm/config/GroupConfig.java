package uk.gov.hmcts.reform.camunda.bpm.config;

public class GroupConfig {

  private String adGroupId;
  private String tenantName;
  private String tenantId;
  private String groupId;
  private String groupName;
  private AccessControl accessControl;

  public String getAdGroupId() {
    return adGroupId;
  }

  public void setAdGroupId(String adGroupId) {
    this.adGroupId = adGroupId;
  }

  public String getTenantName() {
    return tenantName;
  }

  public void setTenantName(String tenantName) {
    this.tenantName = tenantName;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }


  public AccessControl getAccessControl() {
    return accessControl;
  }

  public void setAccessControl(AccessControl accessControl) {
    this.accessControl = accessControl;
  }

}
