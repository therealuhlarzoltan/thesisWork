package hu.uni_obuda.thesis.railways.util.scheduler.repository;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.CronEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.entity.IntervalEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledJob;
import hu.uni_obuda.thesis.railways.util.scheduler.repository.util.JobModifiedEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RequiredArgsConstructor
public class ReactiveCompositeJobRepositoryImpl implements ReactiveCompositeJobRepository {

    private static final long EVENT_HANDLING_TIMEOUT_IN_SECONDS = 10;

    private final ReactiveCrudRepository<JobEntity, Integer> jobRepository;
    private final ReactiveCrudRepository<IntervalEntity, Integer> intervalRepository;
    private final ReactiveCrudRepository<CronEntity, Integer> cronRepository;
    private final List<ScheduledJob> scheduledJobs = new CopyOnWriteArrayList<>();
    private final Scheduler scheduler;
    private final JobModifiedEventHandler modifiedEventHandler;

    private boolean initialized = false;

    public void init() {
       jobRepository.findAll()
            .flatMap(job ->
                    Mono.zip(
                            intervalRepository
                                    .findAll()
                                    .filter(i -> i.getJobId().equals(job.getId()))
                                    .next()
                                    .defaultIfEmpty(null),
                            cronRepository
                                    .findAll()
                                    .filter(c -> c.getJobId().equals(job.getId()))
                                    .collectList(),
                            (interval, crons) -> new ScheduledJob(job, interval, crons)
                    )
            )
            .doOnNext(this::addJobAndLog)
            .doOnError(e -> log.error("CompositeJobRepository initialization failed", e))
            .subscribeOn(scheduler)
            .subscribe(
                    sj -> {},
                    e -> log.error("CompositeJobRepository initialization failed", e),
                    () -> log.info("CompositeJobRepository initialization completed, loaded {} jobs", scheduledJobs.size())
            );
    }

    @Override
    public Mono<Void> handleJobAdded(JobEntity jobEntity) {
       return modifiedEventHandler.onJobAdded(jobEntity, scheduledJobs, Duration.ofSeconds(EVENT_HANDLING_TIMEOUT_IN_SECONDS))
               .doOnError(throwable -> log.error("An exception occurred while handling job added event", throwable))
               .onErrorComplete();
    }

    @Override
    public Mono<Void> onJobModified(JobEntity jobEntity) {
        return modifiedEventHandler.onJobModified(jobEntity, scheduledJobs, Duration.ofSeconds(EVENT_HANDLING_TIMEOUT_IN_SECONDS))
                .doOnError(throwable -> log.error("An exception occurred while handling job modified event", throwable))
                .onErrorComplete();
    }

    @Override
    public Mono<Void> onJobRemoved(Integer jobId) {
        return modifiedEventHandler.onJobRemoved(jobId, scheduledJobs, Duration.ofSeconds(EVENT_HANDLING_TIMEOUT_IN_SECONDS))
                .doOnError(throwable -> log.error("An exception occurred while handling job removed event", throwable))
                .onErrorComplete();
    }

    @Override
    public Flux<ScheduledJob> getScheduledJobs() {
        return Flux.fromIterable(scheduledJobs);
    }

    private void addJobAndLog(ScheduledJob scheduledJob) {
        log.info("Adding scheduled job to the list of jobs: {}", scheduledJob);
        scheduledJobs.add(scheduledJob);
    }
}
