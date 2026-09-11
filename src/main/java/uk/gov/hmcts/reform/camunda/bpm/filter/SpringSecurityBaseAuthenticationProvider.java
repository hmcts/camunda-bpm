package uk.gov.hmcts.reform.camunda.bpm.filter;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.Tenant;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.camunda.bpm.app.AuthorizationHelper;
import uk.gov.hmcts.reform.camunda.bpm.config.AccessControl;
import uk.gov.hmcts.reform.camunda.bpm.config.ConfigProperties;
import uk.gov.hmcts.reform.camunda.bpm.config.GroupConfig;

import java.util.ArrayList;
import java.util.List;


public class SpringSecurityBaseAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityBaseAuthenticationProvider.class);
    public static final String DEFAULT_GROUP_NAME = "All users";
    private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";
    private static final String DEFAULT_GROUP = "default";
    protected ConfigProperties configProperties;

    protected void refreshAuthorisation(AuthorizationHelper authorizationHelper) {
        LOG.debug("Refreshing Camunda authorizations for {} configured groups",
            configProperties.getCamundaGroups().size());
        configProperties.getCamundaGroups().forEach((key, groupConfig) -> {
                AccessControl accessControl = configProperties.getCamundaAccess().get(groupConfig.getAccessControl());
                if (accessControl == null) {
                    LOG.debug("Skipping authorization refresh for mapping key '{}', Camunda group '{}': "
                            + "access-control profile '{}' is not configured",
                        key, groupConfig.getGroupId(), groupConfig.getAccessControl());
                    return;
                }
                LOG.debug("Refreshing authorization for mapping key '{}', Camunda group '{}', tenant '{}', "
                        + "access-control profile '{}' [deployment={}, task={}, processDefinition={}, "
                        + "processInstance={}, batch={}, decisionDefinition={}, optimize={}]",
                    key, groupConfig.getGroupId(), groupConfig.getTenantId(), groupConfig.getAccessControl(),
                    accessControl.isDeploymentAccess(), accessControl.isTaskAccess(),
                    accessControl.isProcessDefinition(), accessControl.isProcessInstance(),
                    accessControl.isBatchAccess(), accessControl.isDecisionDefinitionAccess(),
                    accessControl.isOptimiseAccess());
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
        LOG.debug("Provisioning {} applicable Camunda tenant mappings for user '{}'",
            applicableGroups.size(), id);
        applicableGroups.forEach(groupConfig -> {
                camundaTenants.add(groupConfig.getTenantId());
                long tenantCount = identityService.createTenantQuery().tenantId(groupConfig.getTenantId()).count();
                LOG.debug("Tenant '{}' lookup returned {} existing records for user '{}'",
                    groupConfig.getTenantId(), tenantCount, id);
                if (tenantCount == 0) {
                    Tenant tenant = identityService.newTenant(groupConfig.getTenantId());
                    tenant.setName(groupConfig.getTenantId());
                    identityService.saveTenant(tenant);
                    LOG.debug("Created Camunda tenant '{}' while provisioning user '{}'",
                        groupConfig.getTenantId(), id);
                }

                long membershipCount = identityService.createTenantQuery()
                    .tenantId(groupConfig.getTenantId())
                    .userMember(id)
                    .count();
                LOG.debug("Tenant '{}' membership lookup returned {} records for user '{}'",
                    groupConfig.getTenantId(), membershipCount, id);
                if (membershipCount == 0) {
                    identityService.createTenantUserMembership(groupConfig.getTenantId(), id);
                    LOG.debug("Created membership between user '{}' and Camunda tenant '{}'",
                        id, groupConfig.getTenantId());
                }
            }
        );
        LOG.debug("Finished tenant provisioning for user '{}'; authentication tenants={}", id, camundaTenants);
        return camundaTenants;
    }

    protected List<String> getCamundaGroupsAndProvision(String id, List<GroupConfig> applicableGroups,
                                                        IdentityService identityService) {
        List<String> camundaGroups = new ArrayList<>();

        LOG.debug("Provisioning {} applicable Camunda group mappings for user '{}'; "
                + "configured admin Entra group ID='{}'",
            applicableGroups.size(), id, configProperties.getCamundaAdminGroupId());
        applicableGroups.forEach(groupConfig -> {
                long groupCount = identityService.createGroupQuery().groupId(groupConfig.getGroupId()).count();
                LOG.debug("Processing applicable mapping for user '{}': Entra group ID='{}', Camunda group='{}', "
                        + "tenant='{}', adminGroupMatch={}; Camunda group lookup returned {} records",
                    id, groupConfig.getAdGroupId(), groupConfig.getGroupId(), groupConfig.getTenantId(),
                    groupConfig.getAdGroupId().equals(configProperties.getCamundaAdminGroupId()), groupCount);
                if (groupCount == 0) {
                    Group group = identityService.newGroup(groupConfig.getGroupId());
                    group.setName(groupConfig.getGroupId());
                    identityService.saveGroup(group);
                    LOG.debug("Created Camunda group '{}' while provisioning user '{}'",
                        groupConfig.getGroupId(), id);
                }

                long tenantGroupMembershipCount = identityService.createTenantQuery()
                    .tenantId(groupConfig.getTenantId())
                    .groupMember(groupConfig.getGroupId())
                    .count();
                LOG.debug("Tenant '{}' to Camunda group '{}' membership lookup returned {} records",
                    groupConfig.getTenantId(), groupConfig.getGroupId(), tenantGroupMembershipCount);
                if (tenantGroupMembershipCount == 0) {
                    identityService
                        .createTenantGroupMembership(groupConfig.getTenantId(), groupConfig.getGroupId());
                    LOG.debug("Created membership between Camunda tenant '{}' and group '{}'",
                        groupConfig.getTenantId(), groupConfig.getGroupId());
                }
                identityService.createMembership(id, groupConfig.getGroupId());
                camundaGroups.add(groupConfig.getGroupId());
                LOG.debug("Created membership between user '{}' and Camunda group '{}'",
                    id, groupConfig.getGroupId());

                if (groupConfig.getAdGroupId().equals(configProperties.getCamundaAdminGroupId())) {
                    long adminGroupCount = identityService.createGroupQuery().groupId(CAMUNDA_ADMIN_GROUP).count();
                    LOG.debug("User '{}' matched configured admin Entra group ID '{}'; Camunda admin group lookup "
                            + "returned {} records",
                        id, groupConfig.getAdGroupId(), adminGroupCount);
                    if (adminGroupCount == 0) {
                        Group group = identityService.newGroup(CAMUNDA_ADMIN_GROUP);
                        group.setName(CAMUNDA_ADMIN_GROUP);
                        identityService.saveGroup(group);
                        LOG.debug("Created Camunda admin group '{}'", CAMUNDA_ADMIN_GROUP);
                    }
                    identityService.createMembership(id, CAMUNDA_ADMIN_GROUP);
                    camundaGroups.add(CAMUNDA_ADMIN_GROUP);
                    LOG.debug("Granted Camunda admin membership to user '{}'", id);
                } else {
                    LOG.debug("Mapping did not grant Camunda admin membership to user '{}': Entra group ID '{}' "
                            + "does not equal configured admin Entra group ID '{}'",
                        id, groupConfig.getAdGroupId(), configProperties.getCamundaAdminGroupId());
                }
            }
        );

        long defaultGroupCount = identityService.createGroupQuery().groupId(DEFAULT_GROUP).count();
        LOG.debug("Default Camunda group lookup returned {} records while provisioning user '{}'",
            defaultGroupCount, id);
        if (defaultGroupCount == 0) {
            Group group = identityService.newGroup(DEFAULT_GROUP);
            group.setName(DEFAULT_GROUP_NAME);
            identityService.saveGroup(group);
            LOG.debug("Created default Camunda group '{}'", DEFAULT_GROUP);
        }
        identityService.createMembership(id, DEFAULT_GROUP);
        camundaGroups.add(DEFAULT_GROUP);
        LOG.debug("Created default group membership for user '{}'; final authentication groups={}, isAdmin={}",
            id, camundaGroups, camundaGroups.contains(CAMUNDA_ADMIN_GROUP));

        return camundaGroups;

    }

}
