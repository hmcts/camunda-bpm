package uk.gov.hmcts.reform.camunda.bpm.filter;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import uk.gov.hmcts.reform.camunda.bpm.app.AuthorizationHelper;
import uk.gov.hmcts.reform.camunda.bpm.config.ConfigProperties;
import uk.gov.hmcts.reform.camunda.bpm.config.GroupConfig;
import uk.gov.hmcts.reform.camunda.bpm.context.SpringContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;


@SuppressWarnings("unused")
public class SpringSecurityWebappAuthenticationProvider extends SpringSecurityBaseAuthenticationProvider {

    public static final String GIVEN_NAME = "given_name";
    public static final String FAMILY_NAME = "family_name";
    public static final String UNIQUE_NAME = "unique_name";
    public static final String GROUPS_ATTRIBUTE = "groups";
    public static final String DEFAULT_GROUP_NAME = "All users";
    private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";
    private static final String DEFAULT_GROUP = "default";


    @Override
    @SuppressWarnings("unchecked")
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        configProperties = SpringContext.getAppContext().getBean(ConfigProperties.class);

        if (authentication == null) {
            return AuthenticationResult.unsuccessful();
        }

        String id = authentication.getName();
        if (id == null || id.isEmpty()) {
            return AuthenticationResult.unsuccessful();
        }

        List<OAuth2UserAuthority> authorities = (List<OAuth2UserAuthority>) authentication.getAuthorities();

        Map<String, Object> attributes = authorities.get(0).getAttributes();

        AuthenticationResult authenticationResult = new AuthenticationResult(
            id,
            true
        );


        IdentityService identityService = engine.getIdentityService();
        updateUser(id, attributes, identityService);

        @SuppressWarnings("unchecked")
        List<String> adGroups = (List<String>) attributes.getOrDefault(GROUPS_ATTRIBUTE, emptyList());

        List<GroupConfig> applicableGroups = getCamundaGroupsList(adGroups);

        authenticationResult.setTenants(getTenantsAndProvision(id, applicableGroups, identityService));
        List<String> camundaGroups = getCamundaGroupsAndProvision(id, applicableGroups, identityService);

        if (adGroups.contains(configProperties.getCamundaAdminGroupId())) {
            camundaGroups.add(CAMUNDA_ADMIN_GROUP);
        }
        authenticationResult.setGroups(camundaGroups);
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

    private void updateUser(String id, Map<String, Object> attributes,
                            IdentityService identityService) {

        User user = identityService.newUser(id);
        user.setFirstName(requireNonNull(attributes.get(GIVEN_NAME)).toString());
        user.setLastName(requireNonNull(attributes.get(FAMILY_NAME)).toString());
        user.setEmail(requireNonNull(attributes.get(UNIQUE_NAME)).toString());

        identityService.deleteUser(id);
        identityService.saveUser(user);
    }


    private List<GroupConfig> getCamundaGroupsList(List<String> adGroups) {
        List<GroupConfig> applicableGroups = new ArrayList<>();

        configProperties.getCamundaGroups().forEach((key, groupConfig) -> {
                if (adGroups.contains(groupConfig.getAdGroupId())) {
                    applicableGroups.add(groupConfig);
                }
            }
        );
        return applicableGroups;
    }

}
