package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveDateRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveIntervalRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveJobRepositoryAdapter;
import hu.uni_obuda.thesis.railways.util.scheduler.ReactiveCustomScheduler;
import hu.uni_obuda.thesis.railways.util.scheduler.repository.ReactiveCompositeJobRepository;
import hu.uni_obuda.thesis.railways.util.scheduler.repository.ReactiveCompositeJobRepositoryImpl;
import hu.uni_obuda.thesis.railways.util.scheduler.repository.util.JobModifiedEventHandler;
import hu.uni_obuda.thesis.railways.util.scheduler.scanner.ReactiveScheduledJobScanner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
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

    @Bean
    public JobModifiedEventHandler<ScheduledJobEntity, ScheduledIntervalEntity, ScheduledDateEntity> jobModifiedEventHandler(
            @Lazy ReactiveDateRepositoryAdapter dateRepository,
            @Lazy ReactiveIntervalRepositoryAdapter intervalRepository) {
        return new JobModifiedEventHandler<>(intervalRepository, dateRepository);
    }

    @Bean
    public ReactiveCompositeJobRepository<ScheduledJobEntity> compositeJobRepository(
            @Lazy ReactiveJobRepositoryAdapter jobRepository,
            @Lazy ReactiveDateRepositoryAdapter dateRepository,
            @Lazy ReactiveIntervalRepositoryAdapter intervalRepository,
            @Lazy JobModifiedEventHandler<ScheduledJobEntity, ScheduledIntervalEntity, ScheduledDateEntity> eventHandler,
            @Qualifier("jobRepositoryScheduler") Scheduler scheduler) {
        return new ReactiveCompositeJobRepositoryImpl<>(
                jobRepository,
                intervalRepository,
                dateRepository,
                scheduler,
                eventHandler
        );
    }

    @Bean
    public ReactiveCustomScheduler customScheduler(
            ReactiveScheduledJobScanner jobScanner,
            @Lazy ReactiveCompositeJobRepository<ScheduledJobEntity> jobRepository,
            TaskScheduler taskScheduler,
            @Qualifier("jobRepositoryScheduler") Scheduler repositoryScheduler) {
        return new ReactiveCustomScheduler(jobScanner, jobRepository, repositoryScheduler, taskScheduler);
    }

}
