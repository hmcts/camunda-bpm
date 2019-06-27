package uk.gov.hmcts.app;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/me")
    public OAuth2AuthenticationToken me(OAuth2AuthenticationToken me) {
        return me;
    }
}
