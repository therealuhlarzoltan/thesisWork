package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.scheduler.ScheduledDateMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveDateRepositoryAdapter;
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
class ScheduledDateServiceTest {

    @Mock
    private ReactiveDateRepositoryAdapter dateRepository;
    @Mock
    private ReactiveJobRepositoryAdapter jobRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ScheduledDateMapper mapper;

    @InjectMocks
    private ScheduledDateServiceImpl testedObject;

    @Test
    void findAll_existingDates_mappedResponsesReturned() {
        ScheduledDateEntity e1 = new ScheduledDateEntity();
        e1.setId(1);
        e1.setJobId(10);
        e1.setCronExpression("0 0 * * *");

        ScheduledDateEntity e2 = new ScheduledDateEntity();
        e2.setId(2);
        e2.setJobId(20);
        e2.setCronExpression("0 0 12 * * *");

        ScheduledDateResponse r1 = new ScheduledDateResponse(1, 10, "0 0 * * *");
        ScheduledDateResponse r2 = new ScheduledDateResponse(2, 20, "0 0 12 * * *");

        when(dateRepository.findAll()).thenReturn(Flux.just(e1, e2));
        when(mapper.entityToApi(e1)).thenReturn(r1);
        when(mapper.entityToApi(e2)).thenReturn(r2);

        StepVerifier.create(testedObject.findAll())
                .expectNext(r1, r2)
                .verifyComplete();
    }

    @Test
    void find_existingId_responseReturned() {
        int id = 5;

        ScheduledDateEntity entity = new ScheduledDateEntity();
        entity.setId(id);
        entity.setJobId(10);
        entity.setCronExpression("0 0 * * *");

        ScheduledDateResponse response = new ScheduledDateResponse(id, 10, "0 0 * * *");

        when(dateRepository.findById(id)).thenReturn(Mono.just(entity));
        when(mapper.entityToApi(entity)).thenReturn(response);

        StepVerifier.create(testedObject.find(id))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void find_missingId_entityNotFoundThrown() {
        int id = 42;
        when(dateRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.find(id))
                .expectError(EntityNotFoundException.class)
                .verify();
    }

    @Test
    void create_validRequest_jobExists_dateSavedAndEventPublished() {
        ScheduledDateRequest request = new ScheduledDateRequest(10, "0 0 * * *");

        ScheduledDateEntity entity = new ScheduledDateEntity();
        entity.setId(1);
        entity.setJobId(10);
        entity.setCronExpression("0 0 * * *");

        ScheduledDateResponse response = new ScheduledDateResponse(1, 10, "0 0 * * *");

        ScheduledJobEntity jobEntity = new ScheduledJobEntity();

        when(mapper.apiToEntity(request)).thenReturn(entity);
        when(dateRepository.getKeys()).thenReturn(Flux.empty());
        when(dateRepository.existsByJobIdAndCronExpression(10, "0 0 * * *")).thenReturn(Mono.just(false));
        when(jobRepository.existsById(10)).thenReturn(Mono.just(true));
        when(dateRepository.save(entity)).thenReturn(Mono.just(entity));
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
    void create_existingCronForJob_invalidInputDataThrown() {
        ScheduledDateRequest request = new ScheduledDateRequest(10, "0 0 * * *");

        ScheduledDateEntity entity = new ScheduledDateEntity();
        entity.setId(1);
        entity.setJobId(10);
        entity.setCronExpression("0 0 * * *");

        when(mapper.apiToEntity(request)).thenReturn(entity);
        when(dateRepository.getKeys()).thenReturn(Flux.empty());
        when(dateRepository.existsByJobIdAndCronExpression(10, "0 0 * * *")).thenReturn(Mono.just(true));

        StepVerifier.create(testedObject.create(request))
                .expectError(InvalidInputDataException.class)
                .verify();

        verify(dateRepository, never()).save(any());
        verify(jobRepository, never()).existsById(anyInt());
    }

    @Test
    void create_jobDoesNotExist_entityNotFoundThrown() {
        ScheduledDateRequest request = new ScheduledDateRequest(10, "0 0 * * *");

        ScheduledDateEntity entity = new ScheduledDateEntity();
        entity.setId(1);
        entity.setJobId(10);
        entity.setCronExpression("0 0 * * *");

        when(mapper.apiToEntity(request)).thenReturn(entity);
        when(dateRepository.getKeys()).thenReturn(Flux.empty());
        when(dateRepository.existsByJobIdAndCronExpression(10, "0 0 * * *")).thenReturn(Mono.just(false));
        when(jobRepository.existsById(10)).thenReturn(Mono.just(false));

        StepVerifier.create(testedObject.create(request))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(dateRepository, never()).save(any());
    }

    @Test
    void update_existingDate_uniqueCron_dateUpdatedAndEventPublished() {
        int id = 3;
        int jobId = 20;
        String newCron = "0 5 * * *";

        ScheduledDateRequest request = new ScheduledDateRequest(jobId, newCron);

        ScheduledDateEntity existing = new ScheduledDateEntity();
        existing.setId(id);
        existing.setJobId(jobId);
        existing.setCronExpression("0 0 * * *");

        ScheduledJobEntity jobEntity = new ScheduledJobEntity();

        ScheduledDateResponse mapped = new ScheduledDateResponse(id, jobId, newCron);

        when(dateRepository.findById(id)).thenReturn(Mono.just(existing));
        when(dateRepository.existsByJobIdAndCronExpression(jobId, newCron)).thenReturn(Mono.just(false));
        when(dateRepository.save(existing)).thenReturn(Mono.just(existing));
        when(jobRepository.findById(jobId)).thenReturn(Mono.just(jobEntity));
        when(mapper.entityToApi(existing)).thenReturn(mapped);

        StepVerifier.create(testedObject.update(id, request))
                .expectNext(mapped)
                .verifyComplete();

        assertThat(existing.getCronExpression()).isEqualTo(newCron);

        ArgumentCaptor<ScheduledJobModifiedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledJobModifiedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobEntity()).isEqualTo(jobEntity);
    }

    @Test
    void update_missingDate_entityNotFoundThrown() {
        int id = 99;
        ScheduledDateRequest request = new ScheduledDateRequest(10, "0 0 * * *");

        when(dateRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.update(id, request))
                .expectError(EntityNotFoundException.class)
                .verify();
    }

    @Test
    void update_existingCronForJob_invalidInputDataThrown() {
        int id = 3;
        int jobId = 20;
        String newCron = "0 5 * * *";

        ScheduledDateRequest request = new ScheduledDateRequest(jobId, newCron);

        ScheduledDateEntity existing = new ScheduledDateEntity();
        existing.setId(id);
        existing.setJobId(jobId);
        existing.setCronExpression("0 0 * * *");

        when(dateRepository.findById(id)).thenReturn(Mono.just(existing));
        when(dateRepository.existsByJobIdAndCronExpression(jobId, newCron)).thenReturn(Mono.just(true));

        StepVerifier.create(testedObject.update(id, request))
                .expectError(InvalidInputDataException.class)
                .verify();

        verify(dateRepository, never()).save(any());
        verify(jobRepository, never()).findById(anyInt());
    }

    @Test
    void delete_existingDateAndJob_deletedAndEventPublished() {
        int id = 7;
        int jobId = 30;

        ScheduledDateEntity dateEntity = new ScheduledDateEntity();
        dateEntity.setId(id);
        dateEntity.setJobId(jobId);
        dateEntity.setCronExpression("0 0 * * *");

        ScheduledJobEntity jobEntity = new ScheduledJobEntity();

        when(dateRepository.findById(id)).thenReturn(Mono.just(dateEntity));
        when(dateRepository.delete(dateEntity)).thenReturn(Mono.empty());
        when(jobRepository.findById(jobId)).thenReturn(Mono.just(jobEntity));

        StepVerifier.create(testedObject.delete(id))
                .verifyComplete();

        ArgumentCaptor<ScheduledJobModifiedEvent> eventCaptor = ArgumentCaptor.forClass(ScheduledJobModifiedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobEntity()).isEqualTo(jobEntity);
    }

    @Test
    void delete_missingDate_entityNotFoundThrown() {
        int id = 7;
        when(dateRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.delete(id))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(dateRepository, never()).delete(any());
    }

    @Test
    void delete_existingDateMissingJob_entityNotFoundThrown() {
        int id = 7;
        int jobId = 30;

        ScheduledDateEntity dateEntity = new ScheduledDateEntity();
        dateEntity.setId(id);
        dateEntity.setJobId(jobId);
        dateEntity.setCronExpression("0 0 * * *");

        when(dateRepository.findById(id)).thenReturn(Mono.just(dateEntity));
        when(dateRepository.delete(dateEntity)).thenReturn(Mono.empty());
        when(jobRepository.findById(jobId)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.delete(id))
                .expectError(EntityNotFoundException.class)
                .verify();
    }
}
