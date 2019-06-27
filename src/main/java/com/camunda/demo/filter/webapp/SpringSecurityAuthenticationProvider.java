package com.camunda.demo.filter.webapp;

import java.util.Collection;
import java.util.Map;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

public class SpringSecurityAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    @Override
    @SuppressWarnings("unchecked")
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return AuthenticationResult.unsuccessful();
        }

        String name = authentication.getName();
        if (name == null || name.isEmpty()) {
            return AuthenticationResult.unsuccessful();
        }

        if (((List)authentication.getAuthorities()).get(0) instanceof SimpleGrantedAuthority) {
            return new AuthenticationResult(
                    null,
                    false
            );

        }
        List<OidcUserAuthority> authorities = (List<OidcUserAuthority>) authentication.getAuthorities();

        Map<String, Object> attributes = authorities.get(0).getAttributes();

        String id = name.replaceAll("-_", "");
        AuthenticationResult authenticationResult = new AuthenticationResult(
                id,
                true
        );
        authenticationResult.setGroups(getUserGroups(authentication));

        IdentityService identityService = engine.getIdentityService();
        User user = identityService.newUser(id);
        user.setFirstName(attributes.get("given_name").toString());
        user.setLastName(attributes.get("family_name").toString());
        user.setEmail(attributes.get("upn").toString());

        identityService.deleteUser(id);
        identityService.saveUser(user);

        return authenticationResult;
    }

    private List<String> getUserGroups(Authentication authentication){

        List<String> groupIds;

        groupIds = authentication.getAuthorities().stream()
                .map(res -> res.getAuthority())
                .map(res -> res.substring(5)) // Strip "ROLE_"
                .collect(Collectors.toList());

        groupIds.add("camunda-admin");

        return groupIds;

    }

}
