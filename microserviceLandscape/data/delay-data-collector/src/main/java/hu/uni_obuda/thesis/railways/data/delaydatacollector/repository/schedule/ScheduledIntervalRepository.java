package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledIntervalEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledIntervalRepository extends ReactiveCrudRepository<ScheduledIntervalEntity, Integer> {
}
