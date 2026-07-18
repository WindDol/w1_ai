package cn.winddol.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(PaperIngestionExecutorProperties.class)
public class PaperIngestionExecutorConfig {

    @Bean("paperIngestionExecutor")
    public TaskExecutor paperIngestionExecutor(PaperIngestionExecutorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("paper-ingest-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(value = "paperIngestionHeartbeatScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService paperIngestionHeartbeatScheduler(
            PaperIngestionExecutorProperties properties) {
        int heartbeatThreads = Math.max(1, properties.getMaxPoolSize());
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(
                heartbeatThreads,
                new CustomizableThreadFactory("paper-ingest-heartbeat-"));
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return scheduler;
    }
}
