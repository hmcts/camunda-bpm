package uk.gov.hmcts.reform.camunda.bpm.filter;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringSecurityWebappAuthenticationProviderUnitTest {

    private static final String USER_GROUP_ID = "d4ad367c-5e1d-44cb-a701-85e16217e4b5";
    private static final String ADMIN_GROUP_ID = "e7ea2042-4ced-45dd-8ae3-e051c6551789";

    @Test
    public void should_preserve_standard_list_groups_claim() {
        List<String> result = SpringSecurityWebappAuthenticationProvider.normalizeGroupsClaim(
            List.of(USER_GROUP_ID, ADMIN_GROUP_ID));

        assertThat(result).containsExactly(USER_GROUP_ID, ADMIN_GROUP_ID);
    }

    @Test
    public void should_decode_json_array_inside_groups_claim() {
        List<String> result = SpringSecurityWebappAuthenticationProvider.normalizeGroupsClaim(
            List.of("[\"" + USER_GROUP_ID + "\",\"" + ADMIN_GROUP_ID + "\"]"));

        assertThat(result).containsExactly(USER_GROUP_ID, ADMIN_GROUP_ID);
    }

    @Test
    public void should_flatten_nested_collection_groups_claim() {
        List<String> result = SpringSecurityWebappAuthenticationProvider.normalizeGroupsClaim(
            List.of(List.of(USER_GROUP_ID, ADMIN_GROUP_ID)));

        assertThat(result).containsExactly(USER_GROUP_ID, ADMIN_GROUP_ID);
    }

    @Test
    public void should_return_empty_list_when_groups_claim_is_missing() {
        assertThat(SpringSecurityWebappAuthenticationProvider.normalizeGroupsClaim(null)).isEmpty();
    }
}
