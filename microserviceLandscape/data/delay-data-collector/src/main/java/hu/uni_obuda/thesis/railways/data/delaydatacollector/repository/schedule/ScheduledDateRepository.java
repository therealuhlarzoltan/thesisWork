package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledDateEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ScheduledDateRepository extends ReactiveCrudRepository<ScheduledDateEntity, Integer> {
    Flux<ScheduledDateEntity> findByJobId(Integer jobId);
    Mono<Boolean> existsByJobIdAndCronExpression(Integer jobId, String cronExpression);
}
