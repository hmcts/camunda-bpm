package com.camunda.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2ClientConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private ClientRegistrationRepository clientRegistrationRepository;

	@Autowired
	private OAuth2AuthorizedClientRepository authorizedClientRepository;

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;

//	@Autowired
//	private OAuth2AuthorizationRequestResolver authorizationRequestResolver;

//	@Autowired
//	private AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

//	@Autowired
//	private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf().disable()
				.authorizeRequests()
				.anyRequest().authenticated()
				.and()
				.oauth2Login()

				.clientRegistrationRepository(this.clientRegistrationRepository)
				.authorizedClientRepository(this.authorizedClientRepository)
				.authorizedClientService(this.authorizedClientService)
				.and()
				.oauth2Client()
				.clientRegistrationRepository(this.clientRegistrationRepository)
				.authorizedClientRepository(this.authorizedClientRepository)
				.authorizedClientService(this.authorizedClientService);
//				.authorizationCodeGrant()
//				.authorizationRequestRepository(this.authorizationRequestRepository)
//				.authorizationRequestResolver(this.authorizationRequestResolver)
//				.accessTokenResponseClient(this.accessTokenResponseClient);
	}

}