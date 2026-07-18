package cn.winddol.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "paper.ingestion.executor")
public class PaperIngestionExecutorProperties {
    private int corePoolSize = 2;
    private int maxPoolSize = 4;
    private int queueCapacity = 20;
    private int awaitTerminationSeconds = 30;
}
