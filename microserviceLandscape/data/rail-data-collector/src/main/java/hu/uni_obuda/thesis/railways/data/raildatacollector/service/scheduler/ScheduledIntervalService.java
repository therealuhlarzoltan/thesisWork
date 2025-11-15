package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ScheduledIntervalService {

    Flux<ScheduledIntervalResponse> findAll();

    Mono<ScheduledIntervalResponse> find(int id);

    Mono<ScheduledIntervalResponse> create(ScheduledIntervalRequest interval);

    Mono<ScheduledIntervalResponse> update(int id, ScheduledIntervalRequest interval);

    Mono<Void> delete(int id);
}
