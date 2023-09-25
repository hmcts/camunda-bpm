package uk.gov.hmcts.reform.camunda.bpm.filter;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.Tenant;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import uk.gov.hmcts.reform.camunda.bpm.app.AuthorizationHelper;
import uk.gov.hmcts.reform.camunda.bpm.config.AccessControl;
import uk.gov.hmcts.reform.camunda.bpm.config.ConfigProperties;
import uk.gov.hmcts.reform.camunda.bpm.config.GroupConfig;

import java.util.ArrayList;
import java.util.List;


public class SpringSecurityBaseAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    public static final String DEFAULT_GROUP_NAME = "All users";
    private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";
    private static final String DEFAULT_GROUP = "default";
    protected ConfigProperties configProperties;

    protected void refreshAuthorisation(AuthorizationHelper authorizationHelper) {
        configProperties.getCamundaGroups().forEach((key, groupConfig) -> {
                AccessControl accessControl = configProperties.getCamundaAccess().get(groupConfig.getAccessControl());
                if (accessControl == null) {
                    return;
                }
                if (accessControl.isDeploymentAccess()) {
                    authorizationHelper.deploymentAccess(groupConfig.getGroupId());
                }
                if (accessControl.isTaskAccess()) {
                    authorizationHelper.taskAccess(groupConfig.getGroupId());
                }
                if (accessControl.isProcessDefinition()) {
                    authorizationHelper.processDefinition(groupConfig.getGroupId());
                }
                if (accessControl.isProcessInstance()) {
                    authorizationHelper.processInstance(groupConfig.getGroupId());
                }
                if (accessControl.isBatchAccess()) {
                    authorizationHelper.batchAccess(groupConfig.getGroupId());
                }
                if (accessControl.isDecisionDefinitionAccess()) {
                    authorizationHelper.decisionDefinitionAccess(groupConfig.getGroupId());
                }
                if (accessControl.isOptimiseAccess()) {
                    authorizationHelper.optimiseAccess(groupConfig.getGroupId());
                }
            }
        );
    }
    
    protected List<String> getTenantsAndProvision(String id, List<GroupConfig> applicableGroups,
                                                  IdentityService identityService) {
        List<String> camundaTenants = new ArrayList<>();
        applicableGroups.forEach(groupConfig -> {
                camundaTenants.add(groupConfig.getTenantId());
                if (identityService.createTenantQuery().tenantId(groupConfig.getTenantId()).count() == 0) {
                    Tenant tenant = identityService.newTenant(groupConfig.getTenantId());
                    tenant.setName(groupConfig.getTenantId());
                    identityService.saveTenant(tenant);
                }

                if (identityService.createTenantQuery()
                    .tenantId(groupConfig.getTenantId())
                    .userMember(id)
                    .count() == 0) {
                    identityService.createTenantUserMembership(groupConfig.getTenantId(), id);
                }
            }
        );
        return camundaTenants;
    }

    protected List<String> getCamundaGroupsAndProvision(String id, List<GroupConfig> applicableGroups,
                                                        IdentityService identityService) {
        List<String> camundaGroups = new ArrayList<>();

        applicableGroups.forEach(groupConfig -> {
                if (identityService.createGroupQuery().groupId(groupConfig.getGroupId()).count() == 0) {
                    Group group = identityService.newGroup(groupConfig.getGroupId());
                    group.setName(groupConfig.getGroupId());
                    identityService.saveGroup(group);
                }

                if (identityService.createTenantQuery()
                    .tenantId(groupConfig.getTenantId())
                    .groupMember(groupConfig.getGroupId())
                    .count() == 0) {
                    identityService
                        .createTenantGroupMembership(groupConfig.getTenantId(), groupConfig.getGroupId());
                }
                identityService.createMembership(id, groupConfig.getGroupId());
                camundaGroups.add(groupConfig.getGroupId());

                if (groupConfig.getAdGroupId().equals(configProperties.getCamundaAdminGroupId())){
                    if (identityService.createGroupQuery().groupId(CAMUNDA_ADMIN_GROUP).count() == 0) {
                        Group group = identityService.newGroup(CAMUNDA_ADMIN_GROUP);
                        group.setName(CAMUNDA_ADMIN_GROUP);
                        identityService.saveGroup(group);
                    }
                    identityService.createMembership(id, CAMUNDA_ADMIN_GROUP);
                    camundaGroups.add(CAMUNDA_ADMIN_GROUP);
                }
            }
        );

        if (identityService.createGroupQuery().groupId(DEFAULT_GROUP).count() == 0) {
            Group group = identityService.newGroup(DEFAULT_GROUP);
            group.setName(DEFAULT_GROUP_NAME);
            identityService.saveGroup(group);
        }
        identityService.createMembership(id, DEFAULT_GROUP);
        camundaGroups.add(DEFAULT_GROUP);

        return camundaGroups;

    }

}
