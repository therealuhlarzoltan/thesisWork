package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledIntervalEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ScheduledIntervalRepository extends ReactiveCrudRepository<ScheduledIntervalEntity, Integer> {
    Mono<ScheduledIntervalEntity> findByJobId(Integer id);
    Mono<Boolean> existsByJobId(Integer jobId);
    Mono<Boolean> existsByJobIdAndIntervalInMillis(Integer jobId, Long intervalInMillis);
}
