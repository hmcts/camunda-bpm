package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import java.util.Collections;

@Configuration
@ConditionalOnProperty(prefix = "camunda.ui.auth", name = "enabled", matchIfMissing = false)
@EnableWebSecurity
@Order(100)
public class WebSecurityWebAppConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/health", "/health/liveness").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login()
            .clientRegistrationRepository(this.clientRegistrationRepository)
            .authorizedClientRepository(this.authorizedClientRepository)
            .authorizedClientService(this.authorizedClientService);
    }

    @Bean
    public FilterRegistrationBean<ContainerBasedAuthenticationFilter> containerBasedAuthenticationFilter() {

        FilterRegistrationBean<ContainerBasedAuthenticationFilter> filterRegistration = new FilterRegistrationBean<>();
        filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
        filterRegistration.setInitParameters(Collections.singletonMap("authentication-provider",
            "uk.gov.hmcts.reform.camunda.bpm.filter.SpringSecurityWebappAuthenticationProvider"));
        filterRegistration.setOrder(101); // make sure the filter is registered after the Spring Security Filter Chain
        filterRegistration.addUrlPatterns("/app/*");
        return filterRegistration;
    }
}