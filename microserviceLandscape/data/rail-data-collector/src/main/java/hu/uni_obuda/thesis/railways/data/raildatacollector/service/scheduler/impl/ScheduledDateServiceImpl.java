package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ScheduledDateServiceImpl implements ScheduledDateService {

    @Override
    public Flux<ScheduledDateResponse> findAll() {
        return null;
    }

    @Override
    public Mono<ScheduledDateResponse> find(int id) {
        return null;
    }

    @Override
    public Mono<ScheduledDateResponse> create(ScheduledDateRequest date) {
        return null;
    }

    @Override
    public Mono<ScheduledDateResponse> update(int id, ScheduledDateRequest date) {
        return null;
    }

    @Override
    public Mono<Void> delete(int id) {
        return null;
    }
}
