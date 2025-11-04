package hu.uni_obuda.thesis.railways.util.scheduler;

import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobAddedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobModifiedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobRemovedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledJob;
import hu.uni_obuda.thesis.railways.util.scheduler.repository.ReactiveCompositeJobRepository;
import hu.uni_obuda.thesis.railways.util.scheduler.scanner.ReactiveScheduledJobScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;


@Slf4j
@RequiredArgsConstructor
public class ReactiveCustomScheduler {

    private final ApplicationContext applicationContext;
    private final ReactiveScheduledJobScanner jobScanner;
    private final ReactiveCompositeJobRepository jobRepository;
    private final Scheduler repositoryScheduler;
    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private Flux<Tuple2<String, ScheduledMethodRunnable>> cachedMethods;


    public void startSchedulingAfterEvent(ApplicationEvent event) {
        if (event instanceof ContextStartedEvent) {
            log.info("Scheduling jobs after startup...");
            cachedMethods = jobScanner.scan(applicationContext).cache();
            scheduleJobs(jobRepository.getScheduledJobs(), cachedMethods);
        } else if (event instanceof ScheduledJobEvent scheduledJobEvent) {
            scheduleJobsAfterEvent(scheduledJobEvent);
        } else {
            log.warn("Encountered unexpected event with type {}, not scheduling jobs", event.getClass().getName());
        }
    }

    private void scheduleJobsAfterEvent(ScheduledJobEvent jobModifiedEvent) {
        if (jobModifiedEvent instanceof ScheduledJobAddedEvent) {
            log.info("Scheduling jobs after addition...");
        } else if (jobModifiedEvent instanceof ScheduledJobModifiedEvent) {
            log.info("Scheduling jobs after modification...");
        } else if (jobModifiedEvent instanceof ScheduledJobRemovedEvent) {
            log.info("Scheduling jobs after removal...");
        }
    }

    private void scheduleJobs(Flux<ScheduledJob> jobs, Flux<Tuple2<String, ScheduledMethodRunnable>> methods) {

    }

}
