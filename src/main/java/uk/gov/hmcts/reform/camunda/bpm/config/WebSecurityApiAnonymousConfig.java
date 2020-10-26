package uk.gov.hmcts.reform.camunda.bpm.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@ConditionalOnProperty(prefix = "camunda.api.auth", name = "enabled", matchIfMissing = true, havingValue = "false")
@EnableWebSecurity
@Order(102)
public class WebSecurityApiAnonymousConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests().anyRequest().anonymous().and().httpBasic().disable();
    }
}