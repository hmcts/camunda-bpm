package uk.gov.hmcts.reform.camunda.bpm.filter;

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
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.camunda.bpm.app.AuthorizationHelper;
import uk.gov.hmcts.reform.camunda.bpm.config.ConfigProperties;
import uk.gov.hmcts.reform.camunda.bpm.config.GroupConfig;
import uk.gov.hmcts.reform.camunda.bpm.context.SpringContext;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;


@SuppressWarnings("unused")
public class SpringSecurityApiAuthenticationProvider extends SpringSecurityBaseAuthenticationProvider {

    public static final String AUTHORISATION = "ServiceAuthorization";
    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityApiAuthenticationProvider.class);

    @Override
    @SuppressWarnings("unchecked")
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            AuthTokenValidator authTokenValidator = SpringContext.getAppContext().getBean(AuthTokenValidator.class);
            String bearerToken = "extractBearerToken(request)";
            String serviceName = "unspec-service";//authTokenValidator.getServiceName(bearerToken);
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

            if (authenticationResult.getTenants().size() == 0) {
                return AuthenticationResult.unsuccessful();
            }

            AuthorizationService authorizationService = engine.getAuthorizationService();
            Authorization authorization = authorizationService
                .createNewAuthorization(Authorization.AUTH_TYPE_GRANT);

            AuthorizationHelper authorizationHelper = new AuthorizationHelper(
                engine.getAuthorizationService());

            refreshAuthorisation(authorizationHelper);
            return AuthenticationResult.successful(serviceName);
        } catch (InvalidTokenException | ServiceException exception) {
            return AuthenticationResult.unsuccessful();
        }

    }

    private String extractBearerToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORISATION);
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
