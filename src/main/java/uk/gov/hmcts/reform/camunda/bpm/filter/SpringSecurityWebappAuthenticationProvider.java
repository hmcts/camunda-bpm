package uk.gov.hmcts.reform.camunda.bpm.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;


@SuppressWarnings("unused")
public class SpringSecurityWebappAuthenticationProvider extends SpringSecurityBaseAuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityWebappAuthenticationProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Object>> OBJECT_LIST_TYPE = new TypeReference<>() { };
    public static final String GIVEN_NAME = "given_name";
    public static final String FAMILY_NAME = "family_name";

    public static final String NAME = "name";
    public static final String UNIQUE_NAME = "unique_name";
    public static final String GROUPS_ATTRIBUTE = "groups";
    private static final String CLAIM_NAMES_ATTRIBUTE = "_claim_names";
    private static final String CLAIM_SOURCES_ATTRIBUTE = "_claim_sources";
    public static final String DEFAULT_GROUP_NAME = "All users";
    private static final String DEFAULT_GROUP = "default";


    @Override
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        configProperties = SpringContext.getAppContext().getBean(ConfigProperties.class);
        LOG.debug("Starting Camunda webapp authentication extraction for request method='{}', URI='{}'; "
                + "configuredMappings={}, configuredAdminEntraGroupId='{}'",
            request.getMethod(), request.getRequestURI(), configProperties.getCamundaGroups().size(),
            configProperties.getCamundaAdminGroupId());

        if (authentication == null) {
            LOG.debug("Camunda webapp authentication unsuccessful: Spring Security context has no authentication");
            return AuthenticationResult.unsuccessful();
        }

        String id = authentication.getName();
        LOG.debug("Found Spring Security authentication: type='{}', authenticated={}, principalName='{}', "
                + "authorityCount={}",
            authentication.getClass().getName(), authentication.isAuthenticated(), id,
            authentication.getAuthorities().size());
        if (id == null || id.isEmpty()) {
            LOG.debug("Camunda webapp authentication unsuccessful: principal name is null or empty");
            return AuthenticationResult.unsuccessful();
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        Map<String, Object> attributes = new HashMap<>();

        if (!authorities.isEmpty()) {
            for (GrantedAuthority authority : authorities) {
                LOG.debug("Inspecting authority type='{}', authority='{}' for user '{}'",
                    authority.getClass().getName(), authority.getAuthority(), id);
                if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    LOG.debug("Merging OAuth2 authority attributes for user '{}'; attribute keys={}",
                        id, oauth2UserAuthority.getAttributes().keySet());
                    attributes.putAll(oauth2UserAuthority.getAttributes());
                    id = authentication.getName();
                }
            }
        }
        LOG.debug("Finished extracting OAuth2 attributes for user '{}'; merged attribute keys={}",
            id, attributes.keySet());

        final AuthenticationResult authenticationResult = new AuthenticationResult(
            id,
            true
        );
        
        IdentityService identityService = engine.getIdentityService();
        updateUser(id, attributes, identityService);

        Object rawGroupsClaim = attributes.get(GROUPS_ATTRIBUTE);
        LOG.debug("Entra group claim diagnostics for user '{}': present={}, valueType='{}', groupCount={}, "
                + "overageClaimNamesPresent={}, overageClaimSourcesPresent={}",
            id, rawGroupsClaim != null,
            rawGroupsClaim == null ? null : rawGroupsClaim.getClass().getName(),
            rawGroupsClaim instanceof Collection<?> collection ? collection.size() : 0,
            attributes.containsKey(CLAIM_NAMES_ATTRIBUTE), attributes.containsKey(CLAIM_SOURCES_ATTRIBUTE));
        if (rawGroupsClaim == null && attributes.containsKey(CLAIM_NAMES_ATTRIBUTE)) {
            LOG.debug("No direct groups claim was emitted for user '{}', but '_claim_names' is present (value={}). "
                    + "This can indicate Entra group-claim overage; the application does not currently fetch the "
                    + "groups from Microsoft Graph",
                id, attributes.get(CLAIM_NAMES_ATTRIBUTE));
        }

        List<String> adGroups = normalizeGroupsClaim(rawGroupsClaim);
        LOG.debug("Entra group IDs received for user '{}': {}", id, adGroups);

        List<GroupConfig> applicableGroups = getCamundaGroupsList(adGroups);
        LOG.debug("Resolved {} applicable Camunda group mappings for user '{}': {}",
            applicableGroups.size(), id, applicableGroups.stream().map(GroupConfig::getGroupId).toList());

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

        LOG.debug("Camunda webapp authentication successful for user '{}'; groups={}, tenants={}, isAdmin={}",
            id, authenticationResult.getGroups(), authenticationResult.getTenants(),
            authenticationResult.getGroups().contains("camunda-admin"));
        return authenticationResult;
    }

    static List<String> normalizeGroupsClaim(Object rawGroupsClaim) {
        if (rawGroupsClaim == null) {
            return emptyList();
        }

        List<String> normalizedGroups = new ArrayList<>();
        addNormalizedGroups(rawGroupsClaim, normalizedGroups);
        return normalizedGroups;
    }

    private static void addNormalizedGroups(Object claimValue, List<String> normalizedGroups) {
        if (claimValue instanceof Collection<?> collection) {
            collection.forEach(value -> addNormalizedGroups(value, normalizedGroups));
            return;
        }

        if (claimValue instanceof Object[] values) {
            for (Object value : values) {
                addNormalizedGroups(value, normalizedGroups);
            }
            return;
        }

        if (!(claimValue instanceof String stringValue)) {
            LOG.debug("Ignoring unsupported value in Entra groups claim: valueType='{}'",
                claimValue == null ? null : claimValue.getClass().getName());
            return;
        }

        String trimmedValue = stringValue.trim();
        if (trimmedValue.startsWith("[") && trimmedValue.endsWith("]")) {
            try {
                List<Object> decodedGroups = OBJECT_MAPPER.readValue(trimmedValue, OBJECT_LIST_TYPE);
                LOG.debug("Decoded JSON-encoded Entra groups claim containing {} entries", decodedGroups.size());
                decodedGroups.forEach(value -> addNormalizedGroups(value, normalizedGroups));
                return;
            } catch (JsonProcessingException exception) {
                LOG.debug("Entra groups claim value looked like a JSON array but could not be decoded; "
                    + "retaining it as a scalar value", exception);
            }
        }

        normalizedGroups.add(stringValue);
    }

    private void updateUser(String id, Map<String, Object> attributes,
                            IdentityService identityService) {

        User user = identityService.newUser(id);
        String name = (String) attributes.get(NAME);
        user.setFirstName(getFirstName(attributes, name));
        user.setLastName(getLastName(attributes, name));
        user.setEmail(requireNonNull(attributes.get(UNIQUE_NAME)).toString());

        User userExists = identityService.createUserQuery().userId(id).singleResult();
        LOG.debug("Updating Camunda user '{}'; existingUser={}, givenNameClaimPresent={}, familyNameClaimPresent={}, "
                + "displayNameClaimPresent={}, uniqueNameClaimPresent={}",
            id, userExists != null, attributes.containsKey(GIVEN_NAME), attributes.containsKey(FAMILY_NAME),
            attributes.containsKey(NAME), attributes.containsKey(UNIQUE_NAME));
        if (userExists != null) {
            identityService.deleteUser(id);
            LOG.debug("Deleted existing Camunda user '{}' before refreshing its identity and memberships", id);
        }
        identityService.saveUser(user);
        LOG.debug("Saved refreshed Camunda user '{}'", id);
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

        LOG.debug("Comparing {} Entra group IDs from the token against {} configured Camunda mappings",
            adGroups.size(), configProperties.getCamundaGroups().size());
        configProperties.getCamundaGroups().forEach((key, groupConfig) -> {
                boolean matches = adGroups.contains(groupConfig.getAdGroupId());
                LOG.debug("Entra-to-Camunda mapping comparison: key='{}', configuredEntraGroupId='{}', "
                        + "CamundaGroup='{}', tenant='{}', matchesTokenGroups={}, adminGroupConfigMatch={}",
                    key, groupConfig.getAdGroupId(), groupConfig.getGroupId(), groupConfig.getTenantId(), matches,
                    groupConfig.getAdGroupId().equals(configProperties.getCamundaAdminGroupId()));
                if (matches) {
                    applicableGroups.add(groupConfig);
                }
            }
        );
        if (applicableGroups.isEmpty()) {
            LOG.debug("No configured Camunda mappings matched the Entra group IDs in the user's token");
        }
        return applicableGroups;
    }

}
