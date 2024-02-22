package uk.gov.hmcts.reform.camunda.bpm.config;

import jakarta.servlet.annotation.WebListener;
import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.request.RequestContextListener;
import uk.gov.hmcts.reform.camunda.bpm.filter.SpringSecurityWebappAuthenticationProvider;

import java.util.Collections;

@Configuration
@ConditionalOnProperty(prefix = "camunda.ui.auth", name = "enabled", matchIfMissing = false)
@SuppressWarnings("java:S4507")
//@EnableWebSecurity(debug = true)
//@Order(100)
public class WebSecurityWebAppConfig {

    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityWebAppConfig.class);

    @Bean
    public CommandLineRunner cmdLineRunner(ApplicationContext context) {
        return args -> {
            ServletContextInitializerBeans scib = new ServletContextInitializerBeans(context,
                    FilterRegistrationBean.class, DelegatingFilterProxyRegistrationBean.class);
            System.out.println("----");
            scib.iterator().forEachRemaining(System.out::println);
        };
    }

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Configuration
    @WebListener
    public static class MyRequestContextListener extends RequestContextListener {
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/health/**").permitAll()
                .requestMatchers("/beans/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .clientRegistrationRepository(this.clientRegistrationRepository)
                .authorizedClientRepository(this.authorizedClientRepository)
                .authorizedClientService(this.authorizedClientService));
        LOG.warn("After WebApp SecurityFilter");
        return http.build();

    }


    @Bean
    public FilterRegistrationBean<ContainerBasedAuthenticationFilter> containerBasedAuthenticationFilter() {

        FilterRegistrationBean<ContainerBasedAuthenticationFilter> filterRegistration = new FilterRegistrationBean<>();
        filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
        filterRegistration.setInitParameters(Collections.singletonMap("authentication-provider",
            "uk.gov.hmcts.reform.camunda.bpm.filter.SpringSecurityWebappAuthenticationProvider"));
        //filterRegistration.setOrder(101); //make sure the filter is registered after the Spring Security Filter Chain
        filterRegistration.addUrlPatterns("/app/*");
        LOG.warn("After WebApp FilterReg");
        return filterRegistration;
    }
}
