package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import hu.uni_obuda.thesis.railways.util.scheduler.scanner.ReactiveScheduledJobScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class SchedulerConfig {

    @Bean(name = "jobRepositoryScheduler")
    public Scheduler repositoryScheduler() {
        return Schedulers.parallel();
    }

    @Bean
    public ReactiveScheduledJobScanner scheduledJobScanner(ApplicationContext applicationContext) {
        return new ReactiveScheduledJobScanner(applicationContext);
    }
}
