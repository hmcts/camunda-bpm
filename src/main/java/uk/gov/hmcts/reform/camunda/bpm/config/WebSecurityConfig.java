package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import java.util.Collections;

@Configuration
@ConditionalOnProperty(prefix = "security", name = "enabled", matchIfMissing = true)
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Value("${security.anonymous:false}")
    private boolean allowAnonymous;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if(allowAnonymous) {
            http.csrf().disable()
                .authorizeRequests().anyRequest().anonymous().and().httpBasic().disable();
        } else {
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
    }

    @Bean
    @ConditionalOnProperty(prefix = "security", name = "anonymous", havingValue="false", matchIfMissing = true)
    public FilterRegistrationBean containerBasedAuthenticationFilter() {

        FilterRegistrationBean filterRegistration = new FilterRegistrationBean();
        filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
        filterRegistration.setInitParameters(Collections.singletonMap("authentication-provider",
            "uk.gov.hmcts.reform.camunda.bpm.filter.webapp.SpringSecurityAuthenticationProvider"));
        filterRegistration.setOrder(101); // make sure the filter is registered after the Spring Security Filter Chain
        filterRegistration.addUrlPatterns("/app/*");
        return filterRegistration;
    }
}