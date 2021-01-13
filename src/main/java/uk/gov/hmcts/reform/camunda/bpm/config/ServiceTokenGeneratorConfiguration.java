package uk.gov.hmcts.reform.camunda.bpm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

@Configuration
@Lazy
public class ServiceTokenGeneratorConfiguration {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @Bean
    public AuthTokenGenerator authTokenGenerator(
        @Value("${idam.s2s-auth.secret}") String secret,
        @Value("${idam.s2s-auth.name}") String microService,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(
            secret,
            microService,
            serviceAuthorisationApi
        );
    }
}
