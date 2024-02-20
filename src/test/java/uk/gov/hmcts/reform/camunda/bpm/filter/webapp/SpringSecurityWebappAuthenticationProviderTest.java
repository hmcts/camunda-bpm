package uk.gov.hmcts.reform.camunda.bpm.filter.webapp;

import jakarta.servlet.http.HttpServletRequest;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.camunda.bpm.CamundaApplication;
import uk.gov.hmcts.reform.camunda.bpm.filter.SpringSecurityWebappAuthenticationProvider;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = CamundaApplication.class)
@TestPropertySource(locations = "classpath:application.yaml")
//**Add it to new tests only if needed.** Application startup fails with ENGINE-08043 when running multiple tests.
@DirtiesContext
public class SpringSecurityWebappAuthenticationProviderTest {

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

    @Autowired
    private IdentityService identityService;

    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @Test
    public void shouldbe_Unauthorized_when_nosecuritycontext() {
        SecurityContextHolder.getContext().setAuthentication(null);
        HttpServletRequest request = new MockHttpServletRequest();
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(request,
            processEngine);
        assertThat(result.isAuthenticated()).isFalse();
    }

    @Test
    public void shouldbe_Unauthorized_when_noPrincipalName() {
        getAuthenticationContextWithoutPrincipalName(singletonList("44886fcb-4564-4bf9-98a5-4f7629078223"),"testName");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
                new MockHttpServletRequest(), processEngine);

        assertThat(result.isAuthenticated()).isFalse();
    }

    @Test
    public void shouldbe_having_admingroup_when_having_adminGroupId() {
        getAuthenticationContext(singletonList("44886fcb-4564-4bf9-98a5-4f7629078223"),"adminUser");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
            new MockHttpServletRequest(), processEngine);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getGroups()).contains("default","camunda-admin");
    }

    @Test
    public void shouldbe_authorized_withDefaultGroup_when_nonmapped_GroupId() {
        getAuthenticationContext(singletonList("2a1c93c8-b6f2-11e9-a2a3-2a2ae2dbcce4"),"defaultUser");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
            new MockHttpServletRequest(), processEngine);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getGroups()).contains("default");

        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.APPLICATION, "cockpit")).isTrue();
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.APPLICATION, "tasklist")).isTrue();

        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.OPTIMIZE, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.DECISION_DEFINITION, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.BATCH, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.PROCESS_INSTANCE, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.PROCESS_DEFINITION, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.TASK, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("defaultUser", singletonList("default"), Permissions.ALL,
            Resources.DEPLOYMENT, "*")).isFalse();

    }

    @Test
    public void shouldbe_authorized_withAdminRelevantPermissions_when_having_customAdminGroupId() {
        getAuthenticationContext(singletonList("d6eb4b7b-d156-4cc4-918c-5de9d8e7ad5b"),"cmcadminuser");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
            new MockHttpServletRequest(), processEngine);
        String[] cmcAdminGroups = {"default", "cmc-admin"};

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getGroups()).contains(cmcAdminGroups);
        assertThat(result.getTenants()).contains("cmc");


        assertThat(authorizationService.isUserAuthorized("cmcadminuser", Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.APPLICATION, "cockpit")).isTrue();
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.APPLICATION, "tasklist")).isTrue();
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.DECISION_DEFINITION, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.BATCH, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.PROCESS_INSTANCE, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.PROCESS_DEFINITION, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.TASK, "*")).isTrue();

        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(cmcAdminGroups), Permissions.ALL,
            Resources.DEPLOYMENT, "*")).isFalse();
    }

    @Test
    public void shouldbe_authorized_withRelevantLimitedPermissions_when_having_customGroupId() {
        getAuthenticationContext(singletonList("c43232cc-8f6d-4910-8bd1-47947f7c9a44"),"probatetestuser");
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
            new MockHttpServletRequest(), processEngine);
        String[] probateTestGroups = {"default", "probate-test"};

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getGroups()).contains(probateTestGroups);
        assertThat(result.getTenants()).contains("probate");


        assertThat(authorizationService.isUserAuthorized("probatetestuser", Arrays.asList(probateTestGroups),
            Permissions.ALL, Resources.APPLICATION, "cockpit")).isTrue();
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.APPLICATION, "tasklist")).isTrue();
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.OPTIMIZE, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL, Resources.DECISION_DEFINITION, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("cmcadminuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.BATCH, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.PROCESS_INSTANCE, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.PROCESS_DEFINITION, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.TASK, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("probatetestuser",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.DEPLOYMENT, "*")).isFalse();
    }

    @Test
    public void should_work_without_first_and_last_name_comma_format() {
        getAuthenticationContextWithOutFirstAndLastName(
                singletonList("d6eb4b7b-d156-4cc4-918c-5de9d8e7ad5b"),"cmcadmin", "Lastname, Firstname"
        );
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
                new MockHttpServletRequest(), processEngine);

        User cmcAdmin = identityService.createUserQuery().userId("cmcadmin").singleResult();
        assertThat(cmcAdmin.getFirstName()).isEqualTo("Firstname");
        assertThat(cmcAdmin.getLastName()).isEqualTo("Lastname");

        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    public void should_work_without_first_and_last_name_space_format() {
        getAuthenticationContextWithOutFirstAndLastName(
                singletonList("d6eb4b7b-d156-4cc4-918c-5de9d8e7ad5b"),"cmcadmin", "Firstname Lastname"
        );
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
                new MockHttpServletRequest(), processEngine);
        
        User cmcAdmin = identityService.createUserQuery().userId("cmcadmin").singleResult();
        assertThat(cmcAdmin.getFirstName()).isEqualTo("Firstname");
        assertThat(cmcAdmin.getLastName()).isEqualTo("Lastname");

        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    public void should_work_without_oauth_authority_type() {
        String id = UUID.randomUUID().toString();
        getAuthenticationContextWithoutOAuth2UserAuthority(
                singletonList("d6eb4b7b-d156-4cc4-918c-5de9d8e7ad5b"),"Lastname", "Firstname","admin@admin", id);
        AuthenticationResult result = new SpringSecurityWebappAuthenticationProvider().extractAuthenticatedUser(
                new MockHttpServletRequest(), processEngine);

        User cmcAdmin = identityService.createUserQuery().userId(id).singleResult();
        assertThat(cmcAdmin.getFirstName()).isEqualTo("Firstname");
        assertThat(cmcAdmin.getLastName()).isEqualTo("Lastname");

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getGroups()).contains("default","cmc-admin");
    }


    private Authentication getAuthenticationContextWithoutPrincipalName(List<String> groups, String name) {
        Map<String, Object> attributes = ImmutableMap.of(
            "groups", groups,
            SpringSecurityWebappAuthenticationProvider.GIVEN_NAME,name,
            SpringSecurityWebappAuthenticationProvider.FAMILY_NAME,name,
            SpringSecurityWebappAuthenticationProvider.PREFERRED_USERNAME,name

        );

        Authentication authentication = new OAuth2AuthenticationToken(mock(DefaultOidcUser.class),
                singletonList(new OAuth2UserAuthority(attributes)), "testId");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;

    }


    private Authentication getAuthenticationContextWithoutOAuth2UserAuthority(
            List<String> groups, String lastName, String firstName, String email, String id) {
        Map<String, Object> attributes = ImmutableMap.of(
            "groups", groups,
            SpringSecurityWebappAuthenticationProvider.GIVEN_NAME, firstName,
            SpringSecurityWebappAuthenticationProvider.FAMILY_NAME, lastName,
            SpringSecurityWebappAuthenticationProvider.PREFERRED_USERNAME, email,
            "sub", id
        );
        // Create authorities
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.now().plusSeconds(60), attributes);
        OidcUserInfo userInfo = new OidcUserInfo(attributes);

        // Create DefaultOidcUser
        DefaultOidcUser defaultOidcUser = new DefaultOidcUser(authorities, idToken, userInfo);

        // Create a list of SimpleGrantedAuthority
        Authentication authentication = new OAuth2AuthenticationToken(defaultOidcUser,
                authorities, "testId");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private Authentication getAuthenticationContextWithoutFirstAndLastName(
            List<String> groups,
            String id,
            String displayName
    ) {
        Map<String, Object> attributes = ImmutableMap.of(
                "groups", groups,
                SpringSecurityWebappAuthenticationProvider.NAME, displayName,
                SpringSecurityWebappAuthenticationProvider.PREFERRED_USERNAME, id

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

    private void getAuthenticationContextWithOutFirstAndLastName(List<String> groups, String id, String displayName) {
        Authentication  authentication = getAuthenticationContextWithoutFirstAndLastName(groups, id, displayName);
        when(authentication.getName()).thenReturn(id);
    }

}
