package uk.gov.hmcts.reform.camunda.bpm.config;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.camunda.bpm.config.features.FeatureFlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LaunchDarklyFeatureFlagProviderTest {

    private LDClientInterface ldClient;
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Before
    public void setUp() {
        ldClient = mock(LDClientInterface.class);
        launchDarklyFeatureFlagProvider = new LaunchDarklyFeatureFlagProvider(ldClient);
    }

    @Test
    public void should_get_boolean_feature_flag_value() {
        when(ldClient.boolVariation(
            eq(FeatureFlag.WA_INITIATE_TASKS_ON_CREATE.getKey()),
            any(LDContext.class),
            eq(false)
        )).thenReturn(true);

        boolean flagEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.WA_INITIATE_TASKS_ON_CREATE
        );

        assertThat(flagEnabled).isTrue();
    }

    @Test
    public void should_default_to_false_when_feature_flag_is_missing_or_not_returned() {
        when(ldClient.boolVariation(
            eq(FeatureFlag.WA_INITIATE_TASKS_ON_CREATE.getKey()),
            any(LDContext.class),
            eq(false)
        )).thenReturn(false);

        boolean flagEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.WA_INITIATE_TASKS_ON_CREATE
        );

        assertThat(flagEnabled).isFalse();
    }

    @Test
    public void should_throw_exception_when_feature_flag_is_null() {
        assertThatThrownBy(() -> launchDarklyFeatureFlagProvider.getBooleanValue(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("featureFlag must not be null");
    }
}
