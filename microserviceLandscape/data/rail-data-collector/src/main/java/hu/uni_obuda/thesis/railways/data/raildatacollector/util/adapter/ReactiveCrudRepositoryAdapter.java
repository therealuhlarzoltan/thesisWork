package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveCrudRepositoryAdapter<T, ID> extends ReactiveCrudRepository<T, ID> {

    static final String SCHEDULER_PREFIX = "scheduler";

    Mono<Boolean> existsByJobId(Integer jobId);

    Mono<Boolean> existsByJobIdAndIntervalInMillis(Integer jobId, Long intervalInMillis);

    Mono<Boolean> existsByJobIdAndCronExpression(Integer jobId, String cronExpression);

    Flux<T> findByJobId(Integer jobId);
}
