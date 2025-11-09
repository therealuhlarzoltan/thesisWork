package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ScheduledIntervalService {

    Flux<ScheduledIntervalResponse> findAll();

    Mono<ScheduledIntervalResponse> find(int id);

    Mono<ScheduledIntervalResponse> create(ScheduledIntervalRequest interval);

    Mono<ScheduledIntervalResponse> update(ScheduledIntervalRequest interval);

    Mono<Void> delete(int id);
}
