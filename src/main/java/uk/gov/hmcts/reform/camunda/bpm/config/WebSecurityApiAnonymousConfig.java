package uk.gov.hmcts.reform.camunda.bpm.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(prefix = "camunda.api.auth", name = "enabled", matchIfMissing = true, havingValue = "false")
@EnableWebSecurity
@Order(102)
public class WebSecurityApiAnonymousConfig {

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeRequests(requests -> requests.anyRequest().anonymous())
                .httpBasic(httpBasic -> httpBasic.disable());
        return http.build();
    }
}