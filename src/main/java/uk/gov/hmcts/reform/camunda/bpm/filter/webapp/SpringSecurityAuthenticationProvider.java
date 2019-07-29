package uk.gov.hmcts.reform.camunda.bpm.filter.webapp;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.Tenant;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import uk.gov.hmcts.reform.camunda.bpm.app.AuthorizationHelper;
import uk.gov.hmcts.reform.camunda.bpm.config.AccessControl;
import uk.gov.hmcts.reform.camunda.bpm.config.ConfigProperties;
import uk.gov.hmcts.reform.camunda.bpm.context.SpringContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import static java.util.Collections.emptyList;

@SuppressWarnings("unused")
public class SpringSecurityAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    public static final String GIVEN_NAME = "given_name";
    public static final String FAMILY_NAME = "family_name";
    public static final String UNIQUE_NAME = "unique_name";
    public static final String GROUPS_ATTRIBUTE = "groups";
    public static final String DEFAULT_GROUP_NAME = "All users";
    private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";
    private static final String DEFAULT_GROUP = "default";
    private ConfigProperties configProperties;


    @Override
    @SuppressWarnings("unchecked")
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request,
                                                         ProcessEngine engine) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        configProperties = SpringContext.getAppContext().getBean(ConfigProperties.class);

        if (authentication == null) {
            return AuthenticationResult.unsuccessful();
        }

        String id = authentication.getName();
        if (id == null || id.isEmpty()) {
            return AuthenticationResult.unsuccessful();
        }

        List<OidcUserAuthority> authorities = (List<OidcUserAuthority>) authentication.getAuthorities();

        Map<String, Object> attributes = authorities.get(0).getAttributes();

        AuthenticationResult authenticationResult = new AuthenticationResult(
            id,
            true
        );

        IdentityService identityService = engine.getIdentityService();
        updateUser(id, attributes, identityService);

        @SuppressWarnings("unchecked")
        List<String> adGroups = (List<String>) attributes.getOrDefault(GROUPS_ATTRIBUTE, emptyList());

        authenticationResult.setTenants(getTenantsAndProvision(id, adGroups, identityService));
        authenticationResult.setGroups(getCamundaGroupsAndProvision(id, adGroups, identityService));

        AuthorizationService authorizationService = engine.getAuthorizationService();
        Authorization authorization = authorizationService
            .createNewAuthorization(Authorization.AUTH_TYPE_GRANT);

        AuthorizationHelper authorizationHelper = new AuthorizationHelper(
            engine.getAuthorizationService());

        refreshAuthorisation(authorizationHelper);

        authorizationHelper.cockpitAccess(DEFAULT_GROUP);
        authorizationHelper.tasklistAccess(DEFAULT_GROUP);

        return authenticationResult;
    }

    private void refreshAuthorisation(AuthorizationHelper authorizationHelper) {
        configProperties.getCamundaGroups().forEach((key, groupConfig) -> {
                if (groupConfig.getAccessControl() != null) {
                    AccessControl accessControl = groupConfig.getAccessControl();
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
            }
        );
    }

    private void updateUser(String id, Map<String, Object> attributes,
                            IdentityService identityService) {
        User user = identityService.newUser(id);
        user.setFirstName(attributes.get(GIVEN_NAME).toString());
        user.setLastName(attributes.get(FAMILY_NAME).toString());
        user.setEmail(attributes.get(UNIQUE_NAME).toString());

        identityService.deleteUser(id);
        identityService.saveUser(user);
    }

    private List<String> getTenantsAndProvision(String id, List<String> adGroups,
                                                IdentityService identityService) {
        List<String> camundaTenants = new ArrayList<>();
        configProperties.getCamundaGroups().forEach((key, groupConfig) -> {
                if (adGroups.contains(groupConfig.getAdGroupId())) {
                    camundaTenants.add(groupConfig.getTenantId());
                    if (identityService.createTenantQuery().tenantId(groupConfig.getTenantId()).count() == 0) {
                        Tenant tenant = identityService.newTenant(groupConfig.getTenantId());
                        tenant.setName(groupConfig.getTenantName());
                        identityService.saveTenant(tenant);
                    }

                    if (identityService.createTenantQuery()
                        .tenantId(groupConfig.getTenantId())
                        .userMember(id)
                        .count() == 0) {
                        identityService.createTenantUserMembership(groupConfig.getTenantId(), id);
                    }
                }
            }
        );
        return camundaTenants;
    }

    private List<String> getCamundaGroupsAndProvision(String id, List<String> adGroups,
                                                      IdentityService identityService) {
        List<String> camundaGroups = new ArrayList<>();
        if (adGroups.contains(configProperties.getCamundaAdminGroupId())) {
            camundaGroups.add(CAMUNDA_ADMIN_GROUP);
        }

        configProperties.getCamundaGroups().forEach((key, groupConfig) -> {
                if (adGroups.contains(groupConfig.getAdGroupId())) {
                    if (identityService.createGroupQuery().groupId(groupConfig.getGroupId()).count() == 0) {
                        Group group = identityService.newGroup(groupConfig.getGroupId());
                        group.setName(groupConfig.getGroupName());
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
                }
            }
        );

        if (identityService.createGroupQuery().groupId(DEFAULT_GROUP).count() == 0) {
            Group group = identityService.newGroup(DEFAULT_GROUP);
            group.setName(DEFAULT_GROUP_NAME);
            identityService.saveGroup(group);
        }

        camundaGroups.add(DEFAULT_GROUP);

        return camundaGroups;

    }

}
