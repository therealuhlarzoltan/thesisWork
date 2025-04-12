package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class MessageProcessorConfig {

    @Bean(name = "messageProcessingScheduler")
    public ScheduledExecutorService responseSendingScheduler() {
        return null;
    }
}
