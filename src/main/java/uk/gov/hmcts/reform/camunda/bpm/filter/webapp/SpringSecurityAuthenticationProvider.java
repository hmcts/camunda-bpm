package uk.gov.hmcts.reform.camunda.bpm.filter.webapp;

import java.util.ArrayList;
import java.util.Map;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.Tenant;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import uk.gov.hmcts.reform.camunda.bpm.app.AuthorizationHelper;

import static java.util.Collections.emptyList;

@SuppressWarnings("unused")
public class SpringSecurityAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    private static final String CMC_ADMIN_AD_GROUP = "d6eb4b7b-d156-4cc4-918c-5de9d8e7ad5b";
    private static final String GLOBAL_ADMIN_GROUP = "44886fcb-4564-4bf9-98a5-4f7629078223";
    private static final String CMC_TENANT_ID = "cmc";
    private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";
    private static final String CMC_ADMIN_CAMUNDA_GROUP = "cmc-admin";
    private static final String PROBATE_ADMIN_CAMUNDA_GROUP = "probate-admin";
    private static final String PROBATE_ADMIN_AD_GROUP = "c43232cc-8f6d-4910-8bd1-47947f7c9a44";
    private static final String PROBATE_TENANT_ID = "probate";
    private static final String DEFAULT_GROUP = "default";

    @Override
    @SuppressWarnings("unchecked")
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
        AuthorizationHelper authorizationHelper = new AuthorizationHelper(engine.getAuthorizationService());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

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
        User user = identityService.newUser(id);
        user.setFirstName(attributes.get("given_name").toString());
        user.setLastName(attributes.get("family_name").toString());
        user.setEmail(attributes.get("unique_name").toString());

        identityService.deleteUser(id);
        identityService.saveUser(user);

        @SuppressWarnings("unchecked")
        List<String> adGroups = (List<String>) attributes.getOrDefault("groups", emptyList());

        authenticationResult.setTenants(getTenantsAndProvision(id, adGroups, identityService));
        authenticationResult.setGroups(getCamundaGroupsAndProvision(id, adGroups, identityService));

        AuthorizationService authorizationService = engine.getAuthorizationService();
        Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);

        boolean cmcAdminAuthExists = authorizationService.createAuthorizationQuery().resourceType(Resources.APPLICATION)
                .groupIdIn(CMC_ADMIN_CAMUNDA_GROUP).resourceId("*").count() > 0;
        if (!cmcAdminAuthExists) {
//            authorization.setGroupId(CMC_ADMIN_CAMUNDA_GROUP);
//            authorization.addPermission(Permissions.ACCESS);
//            authorization.setResource(Resources.APPLICATION);
//            authorization.setResourceId("*");
//            authorizationService.saveAuthorization(authorization);
        }

        authorizationHelper.deploymentAccess(CMC_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.taskAccess(CMC_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.processDefinition(CMC_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.processInstance(CMC_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.batchAccess(CMC_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.decisionDefinitionAccess(CMC_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.optimiseAccess(CMC_ADMIN_CAMUNDA_GROUP);

        authorizationHelper.deploymentAccess(PROBATE_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.taskAccess(PROBATE_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.processDefinition(PROBATE_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.processInstance(PROBATE_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.batchAccess(PROBATE_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.decisionDefinitionAccess(PROBATE_ADMIN_CAMUNDA_GROUP);
        authorizationHelper.optimiseAccess(PROBATE_ADMIN_CAMUNDA_GROUP);

        authorizationHelper.cockpitAccess(DEFAULT_GROUP);
        authorizationHelper.tasklistAccess(DEFAULT_GROUP);

        return authenticationResult;
    }

    private List<String> getTenantsAndProvision(String id, List<String> adGroups, IdentityService identityService) {
        List<String> camundaTenants = new ArrayList<>();
        if (adGroups.contains(CMC_ADMIN_AD_GROUP)) {
            camundaTenants.add(CMC_TENANT_ID);
            if (identityService.createTenantQuery().tenantId(CMC_TENANT_ID).count() == 0) {
                Tenant tenant = identityService.newTenant(CMC_TENANT_ID);
                tenant.setName("Civil Money Claims");
                identityService.saveTenant(tenant);
            }

            if (identityService.createTenantQuery()
                    .tenantId(CMC_TENANT_ID)
                    .userMember(id)
                    .count() == 0) {
                identityService.createTenantUserMembership(CMC_TENANT_ID, id);
            }
        }

        if (adGroups.contains(PROBATE_ADMIN_AD_GROUP)) {
            camundaTenants.add(PROBATE_TENANT_ID);
            if (identityService.createTenantQuery().tenantId(PROBATE_TENANT_ID).count() == 0) {
                Tenant tenant = identityService.newTenant(PROBATE_TENANT_ID);
                tenant.setName("Probate");
                identityService.saveTenant(tenant);
            }

            if (identityService.createTenantQuery()
                    .tenantId(PROBATE_TENANT_ID)
                    .userMember(id)
                    .count() == 0) {
                identityService.createTenantUserMembership(PROBATE_TENANT_ID, id);
            }
        }

        return camundaTenants;
    }

    private List<String> getCamundaGroupsAndProvision(String id, List<String> adGroups, IdentityService identityService) {
        List<String> camundaGroups = new ArrayList<>();
        if (adGroups.contains(GLOBAL_ADMIN_GROUP)) {
            camundaGroups.add(CAMUNDA_ADMIN_GROUP);
        }

        if (adGroups.contains(CMC_ADMIN_AD_GROUP)) {
            if (identityService.createGroupQuery().groupId(CMC_ADMIN_CAMUNDA_GROUP).count() == 0) {
                Group group = identityService.newGroup(CMC_ADMIN_CAMUNDA_GROUP);
                group.setName("CMC Admin");
                identityService.saveGroup(group);
            }

            if (identityService.createTenantQuery()
                    .tenantId(CMC_TENANT_ID)
                    .groupMember(CMC_ADMIN_CAMUNDA_GROUP)
                    .count() == 0) {
                identityService.createTenantGroupMembership(CMC_TENANT_ID, CMC_ADMIN_CAMUNDA_GROUP);

            }

//            if (identityService.createUserQuery().memberOfGroup(CMC_ADMIN_CAMUNDA_GROUP).count() == 0) {
                identityService.createMembership(id, CMC_ADMIN_CAMUNDA_GROUP);
//            }
            camundaGroups.add(CMC_ADMIN_CAMUNDA_GROUP);
        }

        if (adGroups.contains(PROBATE_ADMIN_AD_GROUP)) {
            if (identityService.createGroupQuery().groupId(PROBATE_ADMIN_CAMUNDA_GROUP).count() == 0) {
                Group group = identityService.newGroup(PROBATE_ADMIN_CAMUNDA_GROUP);
                group.setName("Probate Admin");
                identityService.saveGroup(group);
            }

            if (identityService.createTenantQuery()
                    .tenantId(PROBATE_TENANT_ID)
                    .groupMember(PROBATE_ADMIN_CAMUNDA_GROUP)
                    .count() == 0) {
                identityService.createTenantGroupMembership(PROBATE_TENANT_ID, PROBATE_ADMIN_CAMUNDA_GROUP);
            }

//            if (identityService.createUserQuery().memberOfGroup(PROBATE_ADMIN_CAMUNDA_GROUP).count() == 0) {
                identityService.createMembership(id, PROBATE_ADMIN_CAMUNDA_GROUP);
//            }

            camundaGroups.add(PROBATE_ADMIN_CAMUNDA_GROUP);
        }

        if (identityService.createGroupQuery().groupId(DEFAULT_GROUP).count() == 0) {
            Group group = identityService.newGroup(DEFAULT_GROUP);
            group.setName("All users");
            identityService.saveGroup(group);
        }

        camundaGroups.add(DEFAULT_GROUP);


        return camundaGroups;

    }

}
