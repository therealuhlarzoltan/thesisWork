package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ScheduledJobServiceImpl implements ScheduledJobService {

    @Override
    public Flux<ScheduledJobResponse> findAll() {
        return null;
    }

    @Override
    public Mono<ScheduledJobResponse> find(int id) {
        return null;
    }

    @Override
    public Mono<ScheduledJobResponse> create(ScheduledJobRequest job) {
        return null;
    }

    @Override
    public Mono<ScheduledJobResponse> update(int id, ScheduledJobRequest job) {
        return null;
    }

    @Override
    public Mono<Void> delete(int id) {
        return null;
    }
}
