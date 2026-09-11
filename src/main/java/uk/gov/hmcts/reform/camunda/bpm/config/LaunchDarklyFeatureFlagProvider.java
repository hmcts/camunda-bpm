package uk.gov.hmcts.reform.camunda.bpm.config;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.camunda.bpm.config.features.FeatureFlag;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class LaunchDarklyFeatureFlagProvider {

    private static final Logger LOG = getLogger(LaunchDarklyFeatureFlagProvider.class);

    private final LDClientInterface ldClient;

    public LaunchDarklyFeatureFlagProvider(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    public boolean getBooleanValue(FeatureFlag featureFlag) {
        requireNonNull(featureFlag, "featureFlag must not be null");
        LOG.info("Attempting to retrieve feature flag '{}' as Boolean", featureFlag.getKey());
        return ldClient.boolVariation(featureFlag.getKey(), createLaunchDarklyContext(), false);
    }

    private LDContext createLaunchDarklyContext() {
        return LDContext.builder("camunda-bpm")
            .set("firstName", "Work Allocation")
            .set("lastName", "Camunda BPM")
            .build();
    }
}
