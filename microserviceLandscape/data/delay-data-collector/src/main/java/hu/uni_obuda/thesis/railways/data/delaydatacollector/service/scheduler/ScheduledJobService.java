package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ScheduledJobService {

    Flux<ScheduledJobResponse> findAll();

    Mono<ScheduledJobResponse> find(int id);

    Mono<ScheduledJobResponse> create(ScheduledJobRequest job);

    Mono<ScheduledJobResponse> update(int id, ScheduledJobRequest job);

    Mono<Void> delete(int id);
}
