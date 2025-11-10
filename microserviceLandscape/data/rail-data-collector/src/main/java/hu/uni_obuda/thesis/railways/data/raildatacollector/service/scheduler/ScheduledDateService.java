package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ScheduledDateService {

    Flux<ScheduledDateResponse> findAll();

    Mono<ScheduledDateResponse> find(int id);

    Mono<ScheduledDateResponse> create(ScheduledDateRequest date);

    Mono<ScheduledDateResponse> update(int id, ScheduledDateRequest date);

    Mono<Void> delete(int id);
}
