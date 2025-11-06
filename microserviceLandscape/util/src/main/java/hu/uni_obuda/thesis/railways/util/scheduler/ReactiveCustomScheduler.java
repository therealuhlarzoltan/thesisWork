package hu.uni_obuda.thesis.railways.util.scheduler;

import hu.uni_obuda.thesis.railways.util.scheduler.model.JobScheduleDelta;
import hu.uni_obuda.thesis.railways.util.scheduler.util.JobHistoryUtil;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobAddedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobModifiedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobRemovedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledJob;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledTaskEntry;
import hu.uni_obuda.thesis.railways.util.scheduler.repository.ReactiveCompositeJobRepository;
import hu.uni_obuda.thesis.railways.util.scheduler.scanner.ReactiveScheduledJobScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@RequiredArgsConstructor
public class ReactiveCustomScheduler {

    private static final int MAX_REPOSITORY_RETRIES = 20;
    private static final Duration INITIAL_BACKOFF_DURATION = Duration.ofMillis(250);
    private static final Duration MAX_BACKOFF_DURATION = Duration.ofSeconds(2);

    private final ApplicationContext applicationContext;
    private final ReactiveScheduledJobScanner jobScanner;
    private final ReactiveCompositeJobRepository jobRepository;
    private final Scheduler repositoryScheduler;
    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledTaskEntry> futures = new ConcurrentHashMap<>();
    private Flux<Tuple2<String, ScheduledMethodRunnable>> cachedMethods;

    public void startSchedulingAfterEvent(ApplicationEvent event) {
        if (event instanceof ContextStartedEvent) {
            log.info("Scheduling jobs after startup...");
            cachedMethods = jobScanner.scan(applicationContext).cache();
            Mono.fromRunnable(() -> scheduleJobs(getScheduledJobsSafely(jobRepository), cachedMethods))
                    .subscribeOn(repositoryScheduler)
                    .subscribe();
        } else if (event instanceof ScheduledJobEvent scheduledJobEvent) {
            scheduleJobsAfterEvent(scheduledJobEvent);
        } else {
            log.warn("Encountered unexpected event with type {}, not scheduling jobs", event.getClass().getName());
        }
    }

    private void scheduleJobsAfterEvent(ScheduledJobEvent jobModifiedEvent) {
        if (jobModifiedEvent instanceof ScheduledJobAddedEvent) {
            log.info("Scheduling jobs after addition...");
            jobRepository.onJobAdded(jobModifiedEvent.getJobEntity())
                    .then(Mono.fromRunnable(() -> scheduleJobs(getScheduledJobsSafely(jobRepository), cachedMethods)))
                    .subscribeOn(repositoryScheduler)
                    .subscribe();
        } else if (jobModifiedEvent instanceof ScheduledJobModifiedEvent) {
            log.info("Scheduling jobs after modification...");
            jobRepository.onJobModified(jobModifiedEvent.getJobEntity())
                    .then(Mono.fromRunnable(() -> scheduleJobs(getScheduledJobsSafely(jobRepository), cachedMethods)))
                    .subscribeOn(repositoryScheduler)
                    .subscribe();
        } else if (jobModifiedEvent instanceof ScheduledJobRemovedEvent) {
            log.info("Scheduling jobs after removal...");
            jobRepository.onJobRemoved(jobModifiedEvent.getJobEntity().getId())
                    .then(Mono.fromRunnable(() -> scheduleJobs(getScheduledJobsSafely(jobRepository), cachedMethods)))
                    .subscribeOn(repositoryScheduler)
                    .subscribe();
        }
    }

    private Flux<ScheduledJob> getScheduledJobsSafely(ReactiveCompositeJobRepository repository) {
        return repository.getScheduledJobs()
                .retryWhen(
                        Retry.backoff(MAX_REPOSITORY_RETRIES, INITIAL_BACKOFF_DURATION)
                                .maxBackoff(MAX_BACKOFF_DURATION)
                                .jitter(0.5)
                                .filter(ex -> ex instanceof IllegalStateException)
                                .doBeforeRetry(sig -> log.warn(
                                        "Scheduled jobs repository not ready yet (attempt {}/{}): {}",
                                        sig.totalRetries() + 1, MAX_REPOSITORY_RETRIES, sig.failure().getMessage()))
                )
                .onErrorResume(ex -> {
                    log.error("Failed to load scheduled jobs after {} attempts. Returning empty.", MAX_REPOSITORY_RETRIES, ex);
                    return Flux.empty();
                });
    }

    private void scheduleJobs(Flux<ScheduledJob> jobs, Flux<Tuple2<String, ScheduledMethodRunnable>> methods) {
        Mono<Map<String, ScheduledMethodRunnable>> methodMapMono = methods.collectMap(Tuple2::getT1, Tuple2::getT2);
        JobHistoryUtil.differenceAsList(jobs, futures)
                .flatMap(deltas -> methodMapMono.map(methodMap -> Tuples.of(deltas, methodMap)))
                .flatMap(tuple -> {
                    List<JobScheduleDelta> deltas = tuple.getT1();
                    Map<String, ScheduledMethodRunnable> methodMap = tuple.getT2();

                    deltas.forEach(delta -> {
                        String jobName = delta.jobName();
                        ScheduledMethodRunnable runnable = methodMap.get(jobName);

                        if (runnable == null) {
                            log.warn("No method found for job '{}', skipping scheduling commands!", jobName);
                            return;
                        }

                        delta.cronsToRemove().forEach(cron -> cancelCron(jobName, cron));

                        delta.cronsToAdd()
                                .forEach(cron -> scheduleCron(jobName, runnable, cron));

                        if (delta.fixedToRemove() != null) {
                            cancelFixedRate(jobName);
                        }

                        if (delta.fixedToAdd() != null) {
                            scheduleFixedRate(jobName, runnable, delta.fixedToAdd());
                        }
                    });

                    // flatMap requires a Publisher
                    return Mono.empty();
                })
                .subscribeOn(repositoryScheduler)
                .subscribe();
    }

    private void scheduleCron(String name, Runnable task, String cron) {
        ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(cron));
        futures.computeIfPresent(name, (k, v) -> {
            v.addCronTask(cron, future);
            return v;
        });
        futures.computeIfAbsent(name, (k) -> {
            var entry = new ScheduledTaskEntry(name);
            entry.addCronTask(cron, future);
            return entry;
        });
    }

    private void scheduleFixedRate(String name, Runnable task, Duration period) {
        cancelFixedRate(name);
        log.info("Scheduling fixed rate job {}...", name);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(task, period);
        futures.computeIfPresent(name, (k, v) -> {
            v.getFixedRateTask().ifPresent(_ -> cancelFixedRate(name));
            v.setFixedRateTask(period, future);
            return v;
        });
        futures.computeIfAbsent(name, (k) -> {
            var entry = new ScheduledTaskEntry(name);
            entry.setFixedRateTask(period, future);
            return entry;
        });
    }

    private void cancelCron(String name, String cron) {
        log.info("Cancelling cron job {} with expression {}...", name, cron);
        Optional.ofNullable(futures.get(name))
                .ifPresent(entry -> {
                    entry.getCronTask(cron).ifPresent(future -> {
                        entry.removeCronTask(cron);
                        future.cancel(false);
                        if (entry.getFixedRateTask().isEmpty() && entry.getCronExpressions().isEmpty()) {
                            futures.remove(name);
                        }
                    });
                });
    }

    private void cancelFixedRate(String name) {
        log.info("Cancelling fixed rate job {}...", name);
        Optional.ofNullable(futures.get(name))
                .ifPresent(entry -> {
                    entry.getFixedRateTask().ifPresent(future -> {
                        entry.setFixedRateTask(null,null);
                        future.cancel(false);
                        if (entry.getFixedRateTask().isEmpty() && entry.getCronExpressions().isEmpty()) {
                            futures.remove(name);
                        }
                    });
                });
    }
}
