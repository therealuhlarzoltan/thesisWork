package hu.uni_obuda.thesis.railways.util.scheduler.repository;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledJob;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveCompositeJobRepository {
    Mono<Void> handleJobAdded(JobEntity jobEntity);

    Mono<Void> onJobModified(JobEntity jobEntity);

    Mono<Void> onJobRemoved(Integer jobId);

    Flux<ScheduledJob> getScheduledJobs();
}
