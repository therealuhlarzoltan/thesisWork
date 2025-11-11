package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.ScheduledDateMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.ScheduledIntervalMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledJobService;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveDateRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveIntervalRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveJobRepositoryAdapter;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobAddedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobModifiedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduledJobServiceImpl implements ScheduledJobService {

    private final ReactiveJobRepositoryAdapter jobRepository;
    private final ReactiveDateRepositoryAdapter dateRepository;
    private final ReactiveIntervalRepositoryAdapter intervalRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledDateMapper dateMapper;
    private final ScheduledIntervalMapper intervalMapper;

    @Override
    public Flux<ScheduledJobResponse> findAll() {
        return jobRepository.findAll().flatMap(this::buildScheduledJobResponse);
    }

    @Override
    public Mono<ScheduledJobResponse> find(int id) {
        return jobRepository.findById(id).flatMap(this::buildScheduledJobResponse);
    }

    @Override
    public Mono<ScheduledJobResponse> create(ScheduledJobRequest job) {

        return jobRepository.save(buildScheduledJobEntity(job))
                .delayUntil(this::publishJobAdded)
                .flatMap(this::buildScheduledJobResponse);
    }

    @Override
    public Mono<ScheduledJobResponse> update(int id, ScheduledJobRequest job) {
        return jobRepository.findById(id)
            .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledJobEntity.class)))
            .map(existing -> updatedScheduledJobEntity(existing, job))
            .flatMap(jobRepository::save)
            .delayUntil(this::publishJobModified)
            .flatMap(this::buildScheduledJobResponse);
    }

    @Override
    public Mono<Void> delete(int id) {
        return jobRepository.findById(id)
            .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledJobEntity.class)))
            .flatMap(jobEntity -> jobRepository.delete(jobEntity).thenReturn(jobEntity))
            .delayUntil(this::publishJobRemoved)
            .then();
    }

    private Mono<ScheduledJobResponse> buildScheduledJobResponse(ScheduledJobEntity jobEntity) {
        Mono<Optional<ScheduledIntervalResponse>> intervalDTOMono = intervalRepository.findByJobId(jobEntity.getId()).next()
                .map(intervalMapper::entityToApi).map(Optional::of).defaultIfEmpty(Optional.empty());
        Mono<List<ScheduledDateResponse>> dateDTOMonoList = dateRepository.findByJobId(jobEntity.getId()).map(dateMapper::entityToApi)
                .collectList();

        return Mono.zip(intervalDTOMono, dateDTOMonoList).map((tuple -> {
            ScheduledIntervalResponse intervalResponse = tuple.getT1().orElse(null);
            List<ScheduledDateResponse> dateResponses = tuple.getT2();
            return new ScheduledJobResponse(jobEntity.getId(), jobEntity.getName(), intervalResponse, dateResponses);
        }));
    }

    private static ScheduledJobEntity buildScheduledJobEntity(ScheduledJobRequest jobRequest) {
        return ScheduledJobEntity.builder().name(jobRequest.getJobName()).build();
    }

    private static ScheduledJobEntity updatedScheduledJobEntity(ScheduledJobEntity jobEntity, ScheduledJobRequest jobRequest) {
        jobEntity.setName(jobRequest.getJobName());
        return jobEntity;
    }

    private Mono<Void> publishJobAdded(ScheduledJobEntity job) {
        return Mono.fromRunnable(() -> eventPublisher.publishEvent(new ScheduledJobAddedEvent<>(this, job)));
    }

    private Mono<Void> publishJobModified(ScheduledJobEntity job) {
        return Mono.fromRunnable(() ->
                eventPublisher.publishEvent(new ScheduledJobModifiedEvent<>(this, job))
        );
    }

    private Mono<Void> publishJobRemoved(ScheduledJobEntity job) {
        return Mono.fromRunnable(() -> eventPublisher.publishEvent(new ScheduledJobRemovedEvent<>(this, job)));
    }
}
