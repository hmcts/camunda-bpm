package uk.gov.hmcts.reform.camunda.bpm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskInitiationExecutorConfiguration {

    @Bean
    public TaskExecutor taskInitiationExecutor(
        @Value("${task-initiation.async.core-pool-size:5}") int corePoolSize,
        @Value("${task-initiation.async.max-pool-size:10}") int maxPoolSize,
        @Value("${task-initiation.async.queue-capacity:500}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("task-initiation-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
