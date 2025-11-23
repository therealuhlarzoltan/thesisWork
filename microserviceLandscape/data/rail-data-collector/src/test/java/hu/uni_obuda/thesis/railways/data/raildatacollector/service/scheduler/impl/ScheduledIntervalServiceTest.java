package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.scheduler.ScheduledIntervalMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveIntervalRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveJobRepositoryAdapter;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobModifiedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledIntervalServiceTest {

    @Mock
    private ReactiveIntervalRepositoryAdapter intervalRepository;
    @Mock
    private ReactiveJobRepositoryAdapter jobRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ScheduledIntervalMapper mapper;

    @InjectMocks
    private ScheduledIntervalServiceImpl testedObject;

    @Test
    void findAll_existingIntervals_mappedResponsesReturned() {
        ScheduledIntervalEntity e1 = new ScheduledIntervalEntity();
        e1.setId(1);
        e1.setJobId(10);
        e1.setIntervalInMillis(60_000L);

        ScheduledIntervalEntity e2 = new ScheduledIntervalEntity();
        e2.setId(2);
        e2.setJobId(20);
        e2.setIntervalInMillis(120_000L);

        ScheduledIntervalResponse r1 = new ScheduledIntervalResponse(1, 60_000L);
        ScheduledIntervalResponse r2 = new ScheduledIntervalResponse(2, 120_000L);

        when(intervalRepository.findAll()).thenReturn(Flux.just(e1, e2));
        when(mapper.entityToApi(e1)).thenReturn(r1);
        when(mapper.entityToApi(e2)).thenReturn(r2);

        StepVerifier.create(testedObject.findAll())
                .expectNext(r1, r2)
                .verifyComplete();
    }

    @Test
    void find_existingJobId_responseReturned() {
        int jobId = 10;

        ScheduledIntervalEntity entity = new ScheduledIntervalEntity();
        entity.setId(1);
        entity.setJobId(jobId);
        entity.setIntervalInMillis(60_000L);

        ScheduledIntervalResponse response = new ScheduledIntervalResponse(1, 60_000L);

        when(intervalRepository.findByJobId(jobId)).thenReturn(Flux.just(entity));
        when(mapper.entityToApi(entity)).thenReturn(response);

        StepVerifier.create(testedObject.find(jobId))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void find_missingJobId_entityNotFoundThrown() {
        int jobId = 42;

        when(intervalRepository.findByJobId(jobId)).thenReturn(Flux.empty());

        StepVerifier.create(testedObject.find(jobId))
                .expectError(EntityNotFoundException.class)
                .verify();
    }

    @Test
    void create_validRequest_jobExists_intervalSavedAndEventPublished() {
        ScheduledIntervalRequest request = new ScheduledIntervalRequest(10, 60_000L);

        ScheduledIntervalEntity entity = new ScheduledIntervalEntity();
        entity.setId(1);
        entity.setJobId(10);
        entity.setIntervalInMillis(60_000L);

        ScheduledIntervalResponse response = new ScheduledIntervalResponse(1, 60_000L);

        ScheduledJobEntity jobEntity = new ScheduledJobEntity();

        when(mapper.apiToEntity(request)).thenReturn(entity);
        when(intervalRepository.getKeys()).thenReturn(Flux.empty());
        when(intervalRepository.existsByJobId(10)).thenReturn(Mono.just(false));
        when(jobRepository.existsById(10)).thenReturn(Mono.just(true));
        when(intervalRepository.save(entity)).thenReturn(Mono.just(entity));
        when(jobRepository.findById(10)).thenReturn(Mono.just(jobEntity));
        when(mapper.entityToApi(entity)).thenReturn(response);

        StepVerifier.create(testedObject.create(request))
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<ScheduledJobModifiedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledJobModifiedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobEntity()).isEqualTo(jobEntity);
    }

    @Test
    void create_existingIntervalForJob_invalidInputDataThrown() {
        ScheduledIntervalRequest request = new ScheduledIntervalRequest(10, 60_000L);

        ScheduledIntervalEntity entity = new ScheduledIntervalEntity();
        entity.setId(1);
        entity.setJobId(10);
        entity.setIntervalInMillis(60_000L);

        when(mapper.apiToEntity(request)).thenReturn(entity);
        when(intervalRepository.getKeys()).thenReturn(Flux.empty());
        when(intervalRepository.existsByJobId(10)).thenReturn(Mono.just(true));

        StepVerifier.create(testedObject.create(request))
                .expectError(InvalidInputDataException.class)
                .verify();

        verify(intervalRepository, never()).save(any());
        verify(jobRepository, never()).existsById(anyInt());
    }

    @Test
    void create_jobDoesNotExist_entityNotFoundThrown() {
        ScheduledIntervalRequest request = new ScheduledIntervalRequest(10, 60_000L);

        ScheduledIntervalEntity entity = new ScheduledIntervalEntity();
        entity.setId(1);
        entity.setJobId(10);
        entity.setIntervalInMillis(60_000L);

        when(mapper.apiToEntity(request)).thenReturn(entity);
        when(intervalRepository.getKeys()).thenReturn(Flux.empty());
        when(intervalRepository.existsByJobId(10)).thenReturn(Mono.just(false));
        when(jobRepository.existsById(10)).thenReturn(Mono.just(false));

        StepVerifier.create(testedObject.create(request))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(intervalRepository, never()).save(any());
    }

    @Test
    void update_existingInterval_uniqueValue_intervalUpdatedAndEventPublished() {
        int id = 3;
        int jobId = 20;
        long newInterval = 120_000L;

        ScheduledIntervalRequest request = new ScheduledIntervalRequest(jobId, newInterval);

        ScheduledIntervalEntity existing = new ScheduledIntervalEntity();
        existing.setId(id);
        existing.setJobId(jobId);
        existing.setIntervalInMillis(60_000L);

        ScheduledJobEntity jobEntity = new ScheduledJobEntity();

        ScheduledIntervalResponse mapped = new ScheduledIntervalResponse(id, newInterval);

        when(intervalRepository.findById(id)).thenReturn(Mono.just(existing));
        when(intervalRepository.existsByJobIdAndIntervalInMillis(jobId, newInterval)).thenReturn(Mono.just(false));
        when(intervalRepository.save(existing)).thenReturn(Mono.just(existing));
        when(jobRepository.findById(jobId)).thenReturn(Mono.just(jobEntity));
        when(mapper.entityToApi(existing)).thenReturn(mapped);

        StepVerifier.create(testedObject.update(id, request))
                .expectNext(mapped)
                .verifyComplete();

        assertThat(existing.getIntervalInMillis()).isEqualTo(newInterval);

        ArgumentCaptor<ScheduledJobModifiedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledJobModifiedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobEntity()).isEqualTo(jobEntity);
    }

    @Test
    void update_missingInterval_entityNotFoundThrown() {
        int id = 99;
        ScheduledIntervalRequest request = new ScheduledIntervalRequest(10, 60_000L);

        when(intervalRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.update(id, request))
                .expectError(EntityNotFoundException.class)
                .verify();
    }

    @Test
    void update_existingIntervalValueForJob_invalidInputDataThrown() {
        int id = 3;
        int jobId = 20;
        long newInterval = 120_000L;

        ScheduledIntervalRequest request = new ScheduledIntervalRequest(jobId, newInterval);

        ScheduledIntervalEntity existing = new ScheduledIntervalEntity();
        existing.setId(id);
        existing.setJobId(jobId);
        existing.setIntervalInMillis(60_000L);

        when(intervalRepository.findById(id)).thenReturn(Mono.just(existing));
        when(intervalRepository.existsByJobIdAndIntervalInMillis(jobId, newInterval)).thenReturn(Mono.just(true));

        StepVerifier.create(testedObject.update(id, request))
                .expectError(InvalidInputDataException.class)
                .verify();

        verify(intervalRepository, never()).save(any());
        verify(jobRepository, never()).findById(anyInt());
    }

    @Test
    void delete_existingIntervalAndJob_deletedAndEventPublished() {
        int id = 7;
        int jobId = 30;

        ScheduledIntervalEntity intervalEntity = new ScheduledIntervalEntity();
        intervalEntity.setId(id);
        intervalEntity.setJobId(jobId);
        intervalEntity.setIntervalInMillis(60_000L);

        ScheduledJobEntity jobEntity = new ScheduledJobEntity();

        when(intervalRepository.findById(id)).thenReturn(Mono.just(intervalEntity));
        when(intervalRepository.delete(intervalEntity)).thenReturn(Mono.empty());
        when(jobRepository.findById(jobId)).thenReturn(Mono.just(jobEntity));

        StepVerifier.create(testedObject.delete(id))
                .verifyComplete();

        ArgumentCaptor<ScheduledJobModifiedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledJobModifiedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobEntity()).isEqualTo(jobEntity);
    }

    @Test
    void delete_missingInterval_entityNotFoundThrown() {
        int id = 7;
        when(intervalRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.delete(id))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(intervalRepository, never()).delete(any());
    }

    @Test
    void delete_existingIntervalMissingJob_entityNotFoundThrown() {
        int id = 7;
        int jobId = 30;

        ScheduledIntervalEntity intervalEntity = new ScheduledIntervalEntity();
        intervalEntity.setId(id);
        intervalEntity.setJobId(jobId);
        intervalEntity.setIntervalInMillis(60_000L);

        when(intervalRepository.findById(id)).thenReturn(Mono.just(intervalEntity));
        when(intervalRepository.delete(intervalEntity)).thenReturn(Mono.empty());
        when(jobRepository.findById(jobId)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.delete(id))
                .expectError(EntityNotFoundException.class)
                .verify();
    }
}
