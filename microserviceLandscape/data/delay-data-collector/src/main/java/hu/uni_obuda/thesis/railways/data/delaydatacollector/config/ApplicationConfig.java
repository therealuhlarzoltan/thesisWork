package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.scheduler.Scheduler;

@EnableScheduling
@Configuration
public class ApplicationConfig {

    @Bean(name = "messageSenderScheduler")
    public Scheduler messageScheduler() {
        return null;
    }

    @Bean(name = "trainDelayProcessorScheduler")
    public Scheduler delayProcessorScheduler() {
        return null;
    }
}
