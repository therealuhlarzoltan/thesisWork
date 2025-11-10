package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledIntervalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ScheduledIntervalServiceImpl implements ScheduledIntervalService {

    @Override
    public Flux<ScheduledIntervalResponse> findAll() {
        return null;
    }

    @Override
    public Mono<ScheduledIntervalResponse> find(int id) {
        return null;
    }

    @Override
    public Mono<ScheduledIntervalResponse> create(ScheduledIntervalRequest interval) {
        return null;
    }

    @Override
    public Mono<ScheduledIntervalResponse> update(int id, ScheduledIntervalRequest interval) {
        return null;
    }

    @Override
    public Mono<Void> delete(int id) {
        return null;
    }
}
