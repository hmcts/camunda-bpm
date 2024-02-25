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
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.camunda.bpm.app.AuthorizationHelper;
import uk.gov.hmcts.reform.camunda.bpm.config.ConfigProperties;
import uk.gov.hmcts.reform.camunda.bpm.config.GroupConfig;
import uk.gov.hmcts.reform.camunda.bpm.context.SpringContext;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SpringSecurityApiAuthenticationProvider extends SpringSecurityBaseAuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityApiAuthenticationProvider.class);

    @Override
    @SuppressWarnings("unchecked")
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {

        try {
            AuthTokenValidator authTokenValidator = SpringContext.getAppContext().getBean(AuthTokenValidator.class);
            String bearerToken = extractBearerToken(request);
            String serviceName = authTokenValidator.getServiceName(bearerToken);
            configProperties = SpringContext.getAppContext().getBean(ConfigProperties.class);
            AuthenticationResult authenticationResult = new AuthenticationResult(
                serviceName,
                true
            );

            IdentityService identityService = engine.getIdentityService();
            updateUser(serviceName, identityService);

            List<GroupConfig> applicableGroups = getCamundaGroupsList(serviceName);

            authenticationResult.setTenants(getTenantsAndProvision(serviceName, applicableGroups, identityService));
            List<String> camundaGroups = getCamundaGroupsAndProvision(serviceName, applicableGroups, identityService);
            authenticationResult.setGroups(camundaGroups);

            if (authenticationResult.getTenants().isEmpty()) {
                return AuthenticationResult.unsuccessful();
            }

            AuthorizationService authorizationService = engine.getAuthorizationService();
            authorizationService
                .createNewAuthorization(Authorization.AUTH_TYPE_GRANT);

            AuthorizationHelper authorizationHelper = new AuthorizationHelper(
                engine.getAuthorizationService());

            refreshAuthorisation(authorizationHelper);
            return authenticationResult;
        } catch (InvalidTokenException | ServiceException exception) {
            LOG.warn("Unsuccessful service authentication", exception);
            return AuthenticationResult.unsuccessful();
        }

    }

    private String extractBearerToken(HttpServletRequest request) {
        String token = request.getHeader(ServiceAuthFilter.AUTHORISATION);
        if (token == null) {
            throw new InvalidTokenException("ServiceAuthorization Token is missing");
        }
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }

    private void updateUser(String serviceName,
                            IdentityService identityService) {
        User user = identityService.newUser(serviceName);
        identityService.deleteUser(serviceName);
        identityService.saveUser(user);
    }

    private List<GroupConfig> getCamundaGroupsList(String serviceName) {
        List<GroupConfig> applicableGroups = new ArrayList<>();

        configProperties.getCamundaGroups().forEach((key, groupConfig) -> {
                if (groupConfig.getS2sServiceNames() != null 
                    && groupConfig.getS2sServiceNames().contains(serviceName)) {
                    applicableGroups.add(groupConfig);
                }
            }
        );
        return applicableGroups;
    }

}
