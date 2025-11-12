package hu.uni_obuda.thesis.railways.util.scheduler.repository.util;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.CronEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.entity.IntervalEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JobModifiedEventHandler {

    private final ReactiveCrudRepository<JobEntity, Integer> jobRepository;
    private final ReactiveCrudRepository<IntervalEntity, Integer> intervalRepository;
    private final ReactiveCrudRepository<CronEntity, Integer> cronRepository;

    public Mono<Void> onJobAdded(JobEntity jobEntity, List<ScheduledJob> jobs, Duration timeout) {
        return buildScheduledJob(jobEntity)
                .doOnNext(scheduledJob -> addAndLog(scheduledJob, jobs))
                .timeout(timeout)
                .then();
    }

    public Mono<Void> onJobModified(JobEntity jobEntity, List<ScheduledJob> jobs, Duration timeout) {
        return buildScheduledJob(jobEntity)
                .doOnNext(scheduledJob -> reAddAndLog(scheduledJob, jobs))
                .timeout(timeout)
                .then();
    }

    public Mono<Void> onJobRemoved(Integer jobId, List<ScheduledJob> jobs, Duration timeout) {
        return Mono.fromRunnable(() -> {
            if (jobs.removeIf(job -> job.getId().equals(jobId))) {
                log.info("Job with id of {} successfully removed from the list of jobs", jobId);
            } else {
                log.warn("Job with id of {} was not found in the list of jobs", jobId);
            }
        }).timeout(timeout).then();
    }

    private Mono<ScheduledJob> buildScheduledJob(JobEntity jobEntity) {
        Integer jobId = jobEntity.getId();
        return Mono.zip(
                        retrieveIntervalEntity(jobId),
                        retrieveCronEntities(jobId)
                )
                .map(t -> new ScheduledJob(jobEntity, t.getT1().orElse(null), t.getT2()))
                .switchIfEmpty(Mono.error(new IllegalStateException("No config found for job " + jobId)));
    }

    private Mono<Optional<IntervalEntity>> retrieveIntervalEntity(Integer jobId) {
        return intervalRepository
                .findAll()
                .filter(intervalEntity -> intervalEntity.getJobId().equals(jobId))
                .next() // take the first (and because of the DB scheme the only) matching item or complete empty
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }

    private Mono<List<CronEntity>> retrieveCronEntities(Integer jobId) {
        return cronRepository
                .findAll()
                .filter(cronEntity -> cronEntity.getJobId().equals(jobId))
                .collectList();
    }

    private void addAndLog(ScheduledJob scheduledJob, List<ScheduledJob> jobs) {
        log.info("Adding job {} to the list of jobs", scheduledJob);
        jobs.add(scheduledJob);
    }

    private void reAddAndLog(ScheduledJob scheduledJob, List<ScheduledJob> jobs) {
        log.info("Re-adding job {} to the list of jobs after modification", scheduledJob);
        jobs.replaceAll(listItem -> listItem.getId().equals(scheduledJob.getId()) ? scheduledJob : listItem);
    }
}
