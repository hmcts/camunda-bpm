package uk.gov.hmcts.reform.camunda.bpm.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import uk.gov.hmcts.reform.camunda.bpm.app.AuthorizationHelper;
import uk.gov.hmcts.reform.camunda.bpm.config.ConfigProperties;
import uk.gov.hmcts.reform.camunda.bpm.config.GroupConfig;
import uk.gov.hmcts.reform.camunda.bpm.context.SpringContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;


@SuppressWarnings("unused")
public class SpringSecurityWebappAuthenticationProvider extends SpringSecurityBaseAuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityWebappAuthenticationProvider.class);

    public static final String GIVEN_NAME = "given_name";
    public static final String FAMILY_NAME = "family_name";

    public static final String NAME = "name";
    public static final String UNIQUE_NAME = "unique_name";
    public static final String GROUPS_ATTRIBUTE = "groups";
    public static final String DEFAULT_GROUP_NAME = "All users";
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

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // Check if the authorities contain ROLE_ANONYMOUS
        boolean hasAnonymousRole = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> role.equals("ROLE_ANONYMOUS"));

        if (hasAnonymousRole) {
            return AuthenticationResult.unsuccessful();
        }
        
        Map<String, Object> attributes = new HashMap<>();
        if (!authorities.isEmpty()) {
            for (GrantedAuthority authority : authorities) {
                if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    attributes.putAll(oauth2UserAuthority.getAttributes());
                }
            }
        }

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

        authenticationResult.setGroups(camundaGroups);
        AuthorizationService authorizationService = engine.getAuthorizationService();
        authorizationService
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
        String name = (String) attributes.get(NAME);
        user.setFirstName(getFirstName(attributes, name));
        user.setLastName(getLastName(attributes, name));
        user.setEmail(requireNonNull(attributes.get(UNIQUE_NAME)).toString());

        identityService.deleteUser(id);
        identityService.saveUser(user);
    }

    private static String getFirstName(Map<String, Object> attributes, String name) {
        String firstName = (String) attributes.get(GIVEN_NAME);
        if (firstName == null) {
            // assumes that if a name has a comma in it is in the format "LastName, FirstName"
            if (name.contains(",")) {
                firstName = name.split(",")[1].trim();
            } else {
                firstName = name.split(" ")[0].trim();
            }
        }
        return firstName;
    }

    private static String getLastName(Map<String, Object> attributes, String name) {
        String lastName = (String) attributes.get(FAMILY_NAME);
        if (lastName == null) {
            // assumes that if a name has a comma in it is in the format "LastName, FirstName"
            if (name.contains(",")) {
                lastName = name.split(",")[0].trim();
            } else {
                lastName = name.split(" ")[1].trim();
            }
        }
        return lastName;
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
