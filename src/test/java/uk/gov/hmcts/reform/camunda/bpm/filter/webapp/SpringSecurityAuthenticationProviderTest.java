package uk.gov.hmcts.reform.camunda.bpm.filter.webapp;

import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.camunda.bpm.CamundaApplication;
import uk.gov.hmcts.reform.camunda.bpm.filter.SpringSecurityWebappAuthenticationProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = CamundaApplication.class)
@TestPropertySource(locations = "classpath:application.yaml")
public class SpringSecurityAuthenticationProviderTest {

    @ClassRule
    public static GenericContainer postgreSQLContainer = new PostgreSQLContainer("postgres:11.4")
        .withDatabaseName("camunda")
        .withUsername("camunda")
        .withPassword("camunda").withExposedPorts(5432);

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @MockBean
    private OAuth2AuthorizedClientService authorizedClientService;


    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private AuthorizationService authorizationService;

    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @Test
    public void shouldbe_Unauthorized_when_nosecuritycontext() {
        SecurityContextHolder.getContext().setAuthentication(null);
        HttpServletRequest request = new MockHttpServletRequest();
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(request,
            processEngine);
        assertThat(result.isAuthenticated()).isEqualTo(false);
    }

    @Test
    public void shouldbe_Unauthorized_when_noPrincipalName() {
        getAuthenticationContextWithoutPrincipalName(singletonList("44886fcb-4564-4bf9-98a5-4f7629078223"),"testName");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
                new MockHttpServletRequest(), processEngine);

        assertThat(result.isAuthenticated()).isEqualTo(false);
    }

    @Test
    public void shouldbe_having_admingroup_when_having_adminGroupId() {
        getAuthenticationContext(singletonList("44886fcb-4564-4bf9-98a5-4f7629078223"),"adminUser");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
            new MockHttpServletRequest(), processEngine);

        assertThat(result.isAuthenticated()).isEqualTo(true);
        assertThat(result.getGroups()).contains("default","camunda-admin");
    }

    @Test
    public void shouldbe_authorized_withDefaultGroup_when_nonmapped_GroupId() {
        getAuthenticationContext(singletonList("2a1c93c8-b6f2-11e9-a2a3-2a2ae2dbcce4"),"defaultUser");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
            new MockHttpServletRequest(), processEngine);

        assertThat(result.isAuthenticated()).isEqualTo(true);
        assertThat(result.getGroups()).contains("default");

        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.APPLICATION, "cockpit")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.APPLICATION, "tasklist")).isEqualTo(true);

        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.OPTIMIZE, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.DECISION_DEFINITION, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.BATCH, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.PROCESS_INSTANCE, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.PROCESS_DEFINITION, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.TASK, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.DEPLOYMENT, "*")).isEqualTo(false);

    }

    @Test
    public void shouldbe_authorized_withAdminRelevantPermissions_when_having_customAdminGroupId() {
        getAuthenticationContext(singletonList("d6eb4b7b-d156-4cc4-918c-5de9d8e7ad5b"),"cmcadminuser");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
            new MockHttpServletRequest(), processEngine);
        String[] cmcAdminGroups = {"default", "cmc-admin"};

        assertThat(result.isAuthenticated()).isEqualTo(true);
        assertThat(result.getGroups()).contains(cmcAdminGroups);
        assertThat(result.getTenants()).contains("cmc");


        assertThat(authorizationService.isUserAuthorized("cmcadminuser", Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.APPLICATION, "cockpit")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.APPLICATION, "tasklist")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.DECISION_DEFINITION, "*")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.BATCH, "*")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.PROCESS_INSTANCE, "*")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.PROCESS_DEFINITION, "*")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.TASK, "*")).isEqualTo(true);

        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.DEPLOYMENT, "*")).isEqualTo(false);
    }

    @Test
    public void shouldbe_authorized_withRelevantLimitedPermissions_when_having_customGroupId() {
        getAuthenticationContext(singletonList("c43232cc-8f6d-4910-8bd1-47947f7c9a44"),"probatetestuser");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
            new MockHttpServletRequest(), processEngine);
        String[] probateTestGroups = {"default", "probate-test"};

        assertThat(result.isAuthenticated()).isEqualTo(true);
        assertThat(result.getGroups()).contains(probateTestGroups);
        assertThat(result.getTenants()).contains("probate");


        assertThat(authorizationService.isUserAuthorized("probatetestuser", Arrays.asList(probateTestGroups),
            Permissions.ALL, Resources.APPLICATION, "cockpit")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.APPLICATION, "tasklist")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.OPTIMIZE, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL, Resources.DECISION_DEFINITION, "*")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.BATCH, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.PROCESS_INSTANCE, "*")).isEqualTo(false);
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.PROCESS_DEFINITION, "*")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.TASK, "*")).isEqualTo(true);
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.DEPLOYMENT, "*")).isEqualTo(false);
    }

    private Authentication getAuthenticationContextWithoutPrincipalName(List<String> groups, String name) {
        Map<String, Object> attributes = ImmutableMap.of(
            "groups", groups,
            SpringSecurityWebappAuthenticationProvider.GIVEN_NAME,name,
            SpringSecurityWebappAuthenticationProvider.FAMILY_NAME,name,
            SpringSecurityWebappAuthenticationProvider.UNIQUE_NAME,name

        );

        Authentication authentication = new OAuth2AuthenticationToken(mock(DefaultOidcUser.class),
                singletonList(new OAuth2UserAuthority(attributes)), "testId");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;

    }

    private void getAuthenticationContext(List<String> groups, String name) {
        Authentication  authentication = getAuthenticationContextWithoutPrincipalName(groups, name);
        when(authentication.getName()).thenReturn(name);

    }
}
