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

    private static final long PER_JOB_TIMEOUT_IN_SECONDS = 5;
    private static final long INIT_TIMEOUT_IN_SECONDS = 30;
    private static final long EVENT_HANDLING_TIMEOUT_IN_SECONDS = 10;

    private final ReactiveCrudRepository<JobEntity, Integer> jobRepository;
    private final ReactiveCrudRepository<IntervalEntity, Integer> intervalRepository;
    private final ReactiveCrudRepository<CronEntity, Integer> cronRepository;
    private final List<ScheduledJob> scheduledJobs = new CopyOnWriteArrayList<>();
    private final Scheduler scheduler;
    private final JobModifiedEventHandler modifiedEventHandler;

    private volatile boolean initialized = false;

    public void init() {
        loadJobs().subscribeOn(scheduler).subscribe();
    }

    @Override
    public Mono<Void> onJobAdded(JobEntity jobEntity) {
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
        return initialized ? Flux.fromIterable(scheduledJobs) : Flux.error(new IllegalStateException("CompositeJobRepository is not (yet) initialized"));
    }

    private Mono<Void> loadJobs() {
        return jobRepository.findAll()
                .flatMap(job -> {
                    Mono<IntervalEntity> intervalMono =
                            intervalRepository.findAll()
                                    .filter(i -> i.getJobId().equals(job.getId()))
                                    .next()
                                    .timeout(Duration.ofSeconds(PER_JOB_TIMEOUT_IN_SECONDS))
                                    .onErrorResume(e -> {
                                        log.error("Interval load timed out/failed for job {}", job.getName());
                                        return Mono.empty();
                                    });

                    Mono<List<CronEntity>> cronsMono =
                            cronRepository.findAll()
                                    .filter(c -> c.getJobId().equals(job.getId()))
                                    .collectList()
                                    .timeout(Duration.ofSeconds(PER_JOB_TIMEOUT_IN_SECONDS))
                                    .onErrorResume(e -> {
                                        log.error("Cron load timed out/failed for job {}", job.getName());
                                        return Mono.just(List.of());
                                    });

                    return Mono.zip(intervalMono.defaultIfEmpty(null), cronsMono)
                            .map(t -> new ScheduledJob(job, t.getT1(), t.getT2()));
                })
                .doOnNext(this::addJobAndLog)
                .timeout(Duration.ofSeconds(INIT_TIMEOUT_IN_SECONDS))
                .doOnError(e -> log.error("CompositeJobRepository initialization failed", e))
                .doOnTerminate(() -> {
                    initialized = true;
                    log.info("CompositeJobRepository initialization completed, loaded {} jobs", scheduledJobs.size());
                })
                .then();
    }

    private void addJobAndLog(ScheduledJob scheduledJob) {
        log.info("Adding scheduled job to the list of jobs: {}", scheduledJob);
        scheduledJobs.add(scheduledJob);
    }
}
