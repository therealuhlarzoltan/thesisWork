package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.ScheduledIntervalMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledIntervalService;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveIntervalRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveJobRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.validator.KeyAlreadyPresentValidator;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobModifiedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ScheduledIntervalServiceImpl implements ScheduledIntervalService {

    private final ReactiveIntervalRepositoryAdapter intervalRepository;
    private final ReactiveJobRepositoryAdapter jobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledIntervalMapper mapper;

    @Override
    public Flux<ScheduledIntervalResponse> findAll() {
        return intervalRepository.findAll().map(mapper::entityToApi);
    }

    @Override
    public Mono<ScheduledIntervalResponse> find(int id) {
        return intervalRepository.findByJobId(id)
                .next()
                .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledIntervalEntity.class)))
                .map(mapper::entityToApi);
    }

    @Override
    public Mono<ScheduledIntervalResponse> create(ScheduledIntervalRequest interval) {
        ScheduledIntervalEntity intervalEntity = mapper.apiToEntity(interval);
        return KeyAlreadyPresentValidator.validate(intervalEntity.getId(), intervalRepository.getKeys())
                .then(intervalRepository.existsByJobId(interval.getJobId()))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new InvalidInputDataException("Scheduled interval for job already exists"));
                    }

                    return jobRepository.existsById(interval.getJobId())
                            .flatMap(jobExists -> {
                                if (!jobExists) {
                                    return Mono.error(new EntityNotFoundException(interval.getJobId(), ScheduledJobEntity.class));
                                }
                                return intervalRepository.save(intervalEntity);
                            });
                })
                .flatMap(savedInterval ->
                        jobRepository.findById(savedInterval.getJobId())
                                .switchIfEmpty(Mono.error(new EntityNotFoundException(savedInterval.getJobId(), ScheduledJobEntity.class)))
                                .delayUntil(this::publishJobModified)
                                .thenReturn(savedInterval)
                )
                .map(mapper::entityToApi);
    }

    @Override
    public Mono<ScheduledIntervalResponse> update(int id, ScheduledIntervalRequest interval) {
        return intervalRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledIntervalEntity.class)))
                .map(intervalEntity -> updateScheduledIntervalEntity(intervalEntity, interval))
                .flatMap(intervalEntity -> {
                        return intervalRepository.existsByJobIdAndIntervalInMillis(intervalEntity.getJobId(), intervalEntity.getIntervalInMillis())
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new InvalidInputDataException("Scheduled interval with that value for job already exists"));
                                }

                                return intervalRepository.save(intervalEntity);
                            });
                })
                .flatMap(savedInterval ->
                    jobRepository.findById(savedInterval.getJobId())
                            .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledJobEntity.class)))
                            .delayUntil(this::publishJobModified)
                            .thenReturn(savedInterval)
                )
                .map(mapper::entityToApi);
    }

    @Override
    public Mono<Void> delete(int id) {
        return intervalRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledIntervalEntity.class)))
                .flatMap(intervalEntity ->
                        intervalRepository.delete(intervalEntity)
                                .then(jobRepository.findById(intervalEntity.getJobId()))
                                .switchIfEmpty(Mono.error(new EntityNotFoundException(intervalEntity.getJobId(), ScheduledJobEntity.class)))
                )
                .delayUntil(this::publishJobModified)
                .then();
    }

    private static ScheduledIntervalEntity updateScheduledIntervalEntity(ScheduledIntervalEntity interval, ScheduledIntervalRequest intervalRequest) {
        interval.setIntervalInMillis(intervalRequest.getIntervalInMillis());
        return interval;
    }

    private Mono<Void> publishJobModified(ScheduledJobEntity job) {
        return Mono.fromRunnable(() ->
                eventPublisher.publishEvent(new ScheduledJobModifiedEvent<>(this, job))
        );
    }
}
