package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledJobEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledJobRepository extends ReactiveCrudRepository<ScheduledJobEntity, Integer> {
}
