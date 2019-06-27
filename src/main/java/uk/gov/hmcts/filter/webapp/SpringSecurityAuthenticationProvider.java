package uk.gov.hmcts.filter.webapp;

import java.util.ArrayList;
import java.util.Map;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
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

import static java.util.Collections.emptyList;


@SuppressWarnings("unused")
public class SpringSecurityAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    private static final String CMC_ADMIN_AD_GROUP = "d6eb4b7b-d156-4cc4-918c-5de9d8e7ad5b";
    private static final String GLOBAL_ADMIN_GROUP = "44886fcb-4564-4bf9-98a5-4f7629078223";
    private static final String CMC_TENANT_ID = "cmc";
    private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";
    public static final String CMC_ADMIN_CAMUNDA_GROUP = "cmc-admin";

    @Override
    @SuppressWarnings("unchecked")
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {

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
                    .count() < 1) {
                identityService.createTenantUserMembership(CMC_TENANT_ID, id);
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
            if (identityService.createGroupQuery().groupId(CMC_ADMIN_CAMUNDA_GROUP).count() < 1) {
                Group group = identityService.newGroup(CMC_ADMIN_CAMUNDA_GROUP);
                group.setName("CMC Admin");
                identityService.saveGroup(group);
            }

            if (identityService.createTenantQuery()
                            .tenantId(CMC_TENANT_ID)
                            .groupMember(CMC_ADMIN_CAMUNDA_GROUP)
                            .count() < 1) {
                identityService.createTenantGroupMembership(CMC_TENANT_ID, CMC_ADMIN_CAMUNDA_GROUP);
            }
        }

        return camundaGroups;

    }

}
