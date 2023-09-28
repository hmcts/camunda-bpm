package uk.gov.hmcts.reform.camunda.bpm.config;

import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.camunda.bpm.data.transformer.JsonDataTypeTransformer;

@Configuration
public class CamundaDataTypeConfig extends DefaultDmnEngineConfiguration {

    @Bean
    public JsonDataTypeTransformer registerDataType() {
        // with a default DMN engine configuration
        DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration
                .createDefaultDmnEngineConfiguration();

        configuration
                .getTransformer()
                .getDataTypeTransformerRegistry()
                .addTransformer("json", new JsonDataTypeTransformer());

        return new JsonDataTypeTransformer();
    }
}
