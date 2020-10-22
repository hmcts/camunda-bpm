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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.camunda.bpm.CamundaApplication;
import uk.gov.hmcts.reform.camunda.bpm.filter.SpringSecurityApiAuthenticationProvider;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = CamundaApplication.class)
@TestPropertySource(locations = "classpath:application.yaml")
@DirtiesContext
public class SpringSecurityApiAuthenticationProviderTest {
    
    public static final String TOKEN = "dummytoken";
    @ClassRule
    public static GenericContainer postgreSQLContainer = new PostgreSQLContainer("postgres:11.4")
        .withDatabaseName("camunda")
        .withUsername("camunda")
        .withPassword("camunda").withExposedPorts(5433);
    
    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private AuthorizationService authorizationService;

    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @MockBean
    private AuthTokenValidator authTokenValidator;

    @Test
    public void shouldbe_Unauthorized_when_noHeader() {
        HttpServletRequest request = new MockHttpServletRequest();
        
        AuthenticationResult result = new SpringSecurityApiAuthenticationProvider().extractAuthenticatedUser(request,
            processEngine);
        assertThat(result.isAuthenticated()).isEqualTo(false);
    }

    @Test
    public void shouldbe_Unauthorized_when_s2sException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SpringSecurityApiAuthenticationProvider.AUTHORISATION,TOKEN);
        when(authTokenValidator.getServiceName(anyString())).thenThrow(InvalidTokenException.class);
        AuthenticationResult result = new SpringSecurityApiAuthenticationProvider().extractAuthenticatedUser(request,
            processEngine);
        assertThat(result.isAuthenticated()).isEqualTo(false);
    }

    @Test
    public void shouldbe_Unauthorized_when_serviceName_not_mapped() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SpringSecurityApiAuthenticationProvider.AUTHORISATION,TOKEN);
        when(authTokenValidator.getServiceName(anyString())).thenReturn("unmapped");
        AuthenticationResult result = new SpringSecurityApiAuthenticationProvider().extractAuthenticatedUser(request,
            processEngine);
        assertThat(result.isAuthenticated()).isEqualTo(false);
    }

    @Test
    public void shouldbe_authorized_when_serviceName_mapped() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SpringSecurityApiAuthenticationProvider.AUTHORISATION,TOKEN);
        when(authTokenValidator.getServiceName(anyString())).thenReturn("cmc-claim-store");
        AuthenticationResult result = new SpringSecurityApiAuthenticationProvider().extractAuthenticatedUser(request,
            processEngine);
        assertThat(result.isAuthenticated()).isTrue();
    }
    
    @Test
    public void shouldbe_authorized_withRelevantLimitedPermissions_when_having_customGroupId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SpringSecurityApiAuthenticationProvider.AUTHORISATION,TOKEN);
        when(authTokenValidator.getServiceName(anyString())).thenReturn("probate-service");
        AuthenticationResult result = new SpringSecurityApiAuthenticationProvider().extractAuthenticatedUser(
            request, processEngine);
        String[] probateTestGroups = {"probate-test"};
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getGroups()).contains(probateTestGroups);
        assertThat(result.getTenants()).contains("probate");


        assertThat(authorizationService.isUserAuthorized("probate-service", Arrays.asList(probateTestGroups),
            Permissions.ALL, Resources.APPLICATION, "cockpit")).isFalse();
        assertThat(authorizationService.isUserAuthorized("probate-service",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.APPLICATION, "tasklist")).isFalse();
        assertThat(authorizationService.isUserAuthorized("probate-service",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.OPTIMIZE, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("probate-service",Arrays.asList(probateTestGroups),
            Permissions.ALL, Resources.DECISION_DEFINITION, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("probate-service",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.BATCH, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("probate-service",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.PROCESS_INSTANCE, "*")).isFalse();
        assertThat(authorizationService.isUserAuthorized("probate-service",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.PROCESS_DEFINITION, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("probate-service",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.TASK, "*")).isTrue();
        assertThat(authorizationService.isUserAuthorized("probate-service",Arrays.asList(probateTestGroups),
            Permissions.ALL,  Resources.DEPLOYMENT, "*")).isFalse();
    }

}
