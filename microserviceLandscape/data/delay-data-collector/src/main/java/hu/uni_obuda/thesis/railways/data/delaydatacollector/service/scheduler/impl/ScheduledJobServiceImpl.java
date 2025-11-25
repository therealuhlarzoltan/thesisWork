package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper.ScheduledDateMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper.ScheduledIntervalMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule.ScheduledDateRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule.ScheduledIntervalRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule.ScheduledJobRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.ScheduledJobService;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobAddedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobModifiedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalEventPublisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduledJobServiceImpl implements ScheduledJobService {

    private final ScheduledJobRepository jobRepository;
    private final ScheduledDateRepository dateRepository;
    private final ScheduledIntervalRepository intervalRepository;
    private final TransactionalOperator transactionalOperator;
    private final TransactionalEventPublisher transactionalEventPublisher;
    private final ScheduledDateMapper dateMapper;
    private final ScheduledIntervalMapper intervalMapper;

    @Override
    public Flux<ScheduledJobResponse> findAll() {
        return jobRepository.findAll().flatMap(this::buildScheduledJobResponse);
    }

    @Override
    public Mono<ScheduledJobResponse> find(int id) {
        return jobRepository.findById(id).flatMap(this::buildScheduledJobResponse).switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledJobEntity.class)));
    }

    @Override
    public Mono<ScheduledJobResponse> create(ScheduledJobRequest job) {
        return transactionalOperator.transactional(
                jobRepository.save(buildScheduledJobEntity(job))
                        .delayUntil(saved ->
                                transactionalEventPublisher.publishEvent(ctx -> new ScheduledJobAddedEvent<>(ctx, saved))
                        )
        ).flatMap(this::buildScheduledJobResponse);
    }

    @Override
    public Mono<ScheduledJobResponse> update(int id, ScheduledJobRequest job) {
        return transactionalOperator.transactional(
            jobRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledJobEntity.class)))
                .map(existing -> updatedScheduledJobEntity(existing, job))
                .flatMap(jobRepository::save)
                .delayUntil(saved ->
                        transactionalEventPublisher.publishEvent(ctx -> new ScheduledJobModifiedEvent<>(ctx, saved)))
        )
        .flatMap(this::buildScheduledJobResponse);
    }

    @Override
    public Mono<Void> delete(int id) {
        return transactionalOperator.transactional(
                jobRepository.findById(id)
                        .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledJobEntity.class)))
                        .flatMap(jobEntity ->
                            jobRepository.delete(jobEntity).thenReturn(jobEntity)
                        )
                        .delayUntil(saved -> transactionalEventPublisher.publishEvent(ctx -> new ScheduledJobRemovedEvent<>(ctx, saved)))
        ).then();
    }

    private Mono<ScheduledJobResponse> buildScheduledJobResponse(ScheduledJobEntity jobEntity) {
        Mono<Optional<ScheduledIntervalResponse>> intervalDTOMono = intervalRepository.findByJobId(jobEntity.getId())
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
}
