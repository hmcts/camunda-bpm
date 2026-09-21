package uk.gov.hmcts.reform.camunda.bpm.consumer;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.camunda.bpm.clients.TaskConfigurationServiceApi;

@Configuration
@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
@EnableFeignClients(clients = TaskConfigurationServiceApi.class)
public class TaskManagementConsumerApplication {
}
