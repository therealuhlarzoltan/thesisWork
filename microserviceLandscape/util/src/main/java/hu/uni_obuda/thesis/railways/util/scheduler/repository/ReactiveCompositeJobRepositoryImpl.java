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
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RequiredArgsConstructor
public class ReactiveCompositeJobRepositoryImpl<J extends JobEntity, I extends IntervalEntity, C extends CronEntity> implements ReactiveCompositeJobRepository<J> {

    private static final long PER_JOB_TIMEOUT_IN_SECONDS = 5;
    private static final long INIT_TIMEOUT_IN_SECONDS = 30;
    private static final long EVENT_HANDLING_TIMEOUT_IN_SECONDS = 10;

    private final ReactiveCrudRepository<J, Integer> jobRepository;
    private final ReactiveCrudRepository<I, Integer> intervalRepository;
    private final ReactiveCrudRepository<C, Integer> cronRepository;
    private final List<ScheduledJob> scheduledJobs = new CopyOnWriteArrayList<>();
    private final Scheduler scheduler;
    private final JobModifiedEventHandler<J, I, C> modifiedEventHandler;

    private volatile boolean initialized = false;

    public void init() {
        log.info("Initializing Composite Job Repository...");
        loadJobs().subscribeOn(scheduler).subscribe();
    }

    @Override
    public Mono<Void> onJobAdded(J jobEntity) {
       return modifiedEventHandler.onJobAdded(jobEntity, scheduledJobs, Duration.ofSeconds(EVENT_HANDLING_TIMEOUT_IN_SECONDS))
               .doOnError(throwable -> log.error("An exception occurred while handling job added event", throwable))
               .onErrorComplete();
    }

    @Override
    public Mono<Void> onJobModified(J jobEntity) {
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
        return Flux.defer(() ->
                initialized ? Flux.fromIterable(scheduledJobs)
                        : Flux.error(new IllegalStateException("CompositeJobRepository is not (yet) initialized"))
        );
    }

    private Mono<Void> loadJobs() {
        log.info("Loading jobs...");
        return jobRepository.findAll()
                .flatMap(job -> {
                    Mono<Optional<I>> intervalMono =
                            intervalRepository.findAll()
                                    .filter(i -> i.getJobId().equals(job.getId()))
                                    .next()
                                    .map(Optional::of)
                                    .defaultIfEmpty(Optional.empty())
                                    .timeout(Duration.ofSeconds(PER_JOB_TIMEOUT_IN_SECONDS))
                                    .onErrorResume(e -> {
                                        log.error("Interval load timed out/failed for job {}", job.getName());
                                        return Mono.just(Optional.empty());
                                    });

                    Mono<List<C>> cronsMono =
                            cronRepository.findAll()
                                    .filter(c -> c.getJobId().equals(job.getId()))
                                    .collectList()
                                    .timeout(Duration.ofSeconds(PER_JOB_TIMEOUT_IN_SECONDS))
                                    .onErrorResume(e -> {
                                        log.error("Cron load timed out/failed for job {}", job.getName());
                                        return Mono.just(List.of());
                                    });

                    return Mono.zip(intervalMono, cronsMono)
                            .map(tuple -> new ScheduledJob(job, tuple.getT1().orElse(null), tuple.getT2()));
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
