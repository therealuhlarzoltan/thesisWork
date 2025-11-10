package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledDateEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper.ScheduledDateMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule.ScheduledDateRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.schedule.ScheduledJobRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.ScheduledDateService;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobModifiedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalEventPublisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ScheduledDateServiceImpl implements ScheduledDateService {

    private final ScheduledDateRepository dateRepository;
    private final ScheduledJobRepository jobRepository;
    private final TransactionalOperator transactionalOperator;
    private final TransactionalEventPublisher transactionalEventPublisher;
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
        return transactionalOperator.transactional(
                dateRepository.existsByJobIdAndCronExpression(date.getJobId(), date.getCronExpression())
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
                                        .delayUntil(job -> transactionalEventPublisher.publishEvent(
                                                ctx -> new ScheduledJobModifiedEvent<>(ctx, job)))
                                        .thenReturn(savedDate)
                        )
        ).map(mapper::entityToApi);
    }

    @Override
    public Mono<ScheduledDateResponse> update(int id, ScheduledDateRequest date) {
        return transactionalOperator.transactional(
                dateRepository.findById(id)
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
                                        .delayUntil(job ->
                                                transactionalEventPublisher.publishEvent(ctx -> new ScheduledJobModifiedEvent<>(ctx, job)))
                                        .thenReturn(savedDate)
                        )
        ).map(mapper::entityToApi);
    }

    @Override
    public Mono<Void> delete(int id) {
        return transactionalOperator.transactional(
                dateRepository.findById(id)
                        .switchIfEmpty(Mono.error(new EntityNotFoundException(id, ScheduledDateEntity.class)))
                        .flatMap(intervalEntity ->
                                dateRepository.delete(intervalEntity)
                                        .then(jobRepository.findById(intervalEntity.getJobId()))
                                        .switchIfEmpty(Mono.error(new EntityNotFoundException(intervalEntity.getJobId(), ScheduledJobEntity.class)))
                        )
                        .delayUntil(job -> transactionalEventPublisher.publishEvent(ctx -> new ScheduledJobModifiedEvent<>(ctx, job)))
        ).then();
    }

    private static ScheduledDateEntity updateScheduledDateEntity(ScheduledDateEntity date, ScheduledDateRequest dateRequest) {
        date.setCronExpression(dateRequest.getCronExpression());
        return date;
    }
}
