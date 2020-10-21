package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

@Configuration
@ConditionalOnProperty(prefix = "camunda.api.auth", name = "enabled", matchIfMissing = true)
@EnableWebSecurity
@Order(101)
public class WebSecurityApiConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests().anyRequest().anonymous().and().httpBasic().disable();
    }

    @Bean
    public FilterRegistrationBean<ProcessEngineAuthenticationFilter> authenticationFilter() {
        FilterRegistrationBean<ProcessEngineAuthenticationFilter> filterRegistration = new FilterRegistrationBean<>();
        filterRegistration.setFilter(new ProcessEngineAuthenticationFilter());
        filterRegistration.addInitParameter("authentication-provider",
            "uk.gov.hmcts.reform.camunda.bpm.filter.SpringSecurityApiAuthenticationProvider");
        filterRegistration.setOrder(102); // make sure the filter is registered after the Spring Security Filter Chain
        filterRegistration.addUrlPatterns("/engine-rest/*");
        return filterRegistration;
    }


    @Bean
    public AuthTokenValidator authTokenValidator(ServiceAuthorisationApi authorisationApi) {
        return new ServiceAuthTokenValidator(authorisationApi);
    }


}