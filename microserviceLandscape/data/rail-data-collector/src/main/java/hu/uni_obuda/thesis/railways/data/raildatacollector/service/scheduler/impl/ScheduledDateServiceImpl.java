package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.ScheduledDateMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledDateService;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveDateRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveJobRepositoryAdapter;
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
public class ScheduledDateServiceImpl implements ScheduledDateService {

    private final ReactiveDateRepositoryAdapter dateRepository;
    private final ReactiveJobRepositoryAdapter jobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledDateMapper mapper;

    @Override
    public Flux<ScheduledDateResponse> findAll() {
        return dateRepository.findAll().map(mapper::entityToApi);
    }

    @Override
    public Mono<ScheduledDateResponse> find(int id) {
        return dateRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledDateEntity.class)))
                .map(mapper::entityToApi);
    }

    @Override
    public Mono<ScheduledDateResponse> create(ScheduledDateRequest date) {
        return dateRepository.existsByJobIdAndCronExpression(date.getJobId(), date.getCronExpression())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new InvalidInputDataException("Scheduled date with cron expression for job already exists"));
                    }

                    return jobRepository.existsById(date.getJobId())
                            .flatMap(jobExists -> {
                                if (!jobExists) {
                                    return Mono.error(new EntityNotFoundException(date.getJobId(), ScheduledJobEntity.class));
                                }
                                return dateRepository.save(mapper.apiToEntity(date));
                            });
                })
                .flatMap(savedDate ->
                        jobRepository.findById(savedDate.getJobId())
                                .switchIfEmpty(Mono.error(new EntityNotFoundException(savedDate.getJobId(), ScheduledJobEntity.class)))
                                .delayUntil(this::publishJobModified)
                                .thenReturn(savedDate)
                )
                .map(mapper::entityToApi);
    }

    @Override
    public Mono<ScheduledDateResponse> update(int id, ScheduledDateRequest date) {
        return dateRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledDateEntity.class)))
                .map(dateEntity -> updateScheduledDateEntity(dateEntity, date))
                .flatMap(dateEntity -> {
                    return dateRepository.existsByJobIdAndCronExpression(dateEntity.getJobId(), dateEntity.getCronExpression())
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new InvalidInputDataException("Scheduled date with cron expression for job already exists"));
                                }

                                return dateRepository.save(dateEntity);
                            });
                })
                .flatMap(savedDate ->
                        jobRepository.findById(savedDate.getJobId())
                                .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledJobEntity.class)))
                                .delayUntil(this::publishJobModified)
                                .thenReturn(savedDate)
                )
                .map(mapper::entityToApi);
    }

    @Override
    public Mono<Void> delete(int id) {
        return dateRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledDateEntity.class)))
                .flatMap(intervalEntity ->
                        dateRepository.delete(intervalEntity)
                                .then(jobRepository.findById(intervalEntity.getJobId()))
                                .switchIfEmpty(Mono.error(new EntityNotFoundException(intervalEntity.getJobId(), ScheduledJobEntity.class)))
                )
                .delayUntil(this::publishJobModified)
                .then();
    }

    private static ScheduledDateEntity updateScheduledDateEntity(ScheduledDateEntity date, ScheduledDateRequest dateRequest) {
        date.setCronExpression(dateRequest.getCronExpression());
        return date;
    }

    private Mono<Void> publishJobModified(ScheduledJobEntity job) {
        return Mono.fromRunnable(() ->
                eventPublisher.publishEvent(new ScheduledJobModifiedEvent<>(this, job))
        );
    }
}