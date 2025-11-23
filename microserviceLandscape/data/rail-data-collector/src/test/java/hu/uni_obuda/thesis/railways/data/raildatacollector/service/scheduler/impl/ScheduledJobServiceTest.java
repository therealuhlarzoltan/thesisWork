package hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.impl;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.scheduler.ScheduledDateMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.scheduler.ScheduledIntervalMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveDateRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveIntervalRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveJobRepositoryAdapter;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobAddedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobModifiedEvent;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobRemovedEvent;
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
class ScheduledJobServiceTest {

    @Mock
    private ReactiveJobRepositoryAdapter jobRepository;
    @Mock
    private ReactiveDateRepositoryAdapter dateRepository;
    @Mock
    private ReactiveIntervalRepositoryAdapter intervalRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ScheduledDateMapper dateMapper;
    @Mock
    private ScheduledIntervalMapper intervalMapper;

    @InjectMocks
    private ScheduledJobServiceImpl testedObject;

    @Test
    void findAll_jobsWithIntervalsAndDates_responsesAggregated() {
        ScheduledJobEntity job1 = new ScheduledJobEntity();
        job1.setId(1);
        job1.setName("Job-1");

        ScheduledIntervalEntity interval1 = new ScheduledIntervalEntity();
        interval1.setId(101);
        interval1.setJobId(1);
        interval1.setIntervalInMillis(60_000L);

        ScheduledIntervalResponse intervalResp1 = new ScheduledIntervalResponse(101, 60_000L);

        ScheduledDateEntity date11 = new ScheduledDateEntity();
        date11.setId(201);
        date11.setJobId(1);
        date11.setCronExpression("0 0 * * *");

        ScheduledDateEntity date12 = new ScheduledDateEntity();
        date12.setId(202);
        date12.setJobId(1);
        date12.setCronExpression("0 12 * * *");

        ScheduledDateResponse dateResp11 = new ScheduledDateResponse(201, 1, "0 0 * * *");
        ScheduledDateResponse dateResp12 = new ScheduledDateResponse(202, 1, "0 12 * * *");

        ScheduledJobEntity job2 = new ScheduledJobEntity();
        job2.setId(2);
        job2.setName("Job-2");

        ScheduledDateEntity date21 = new ScheduledDateEntity();
        date21.setId(301);
        date21.setJobId(2);
        date21.setCronExpression("0 6 * * *");

        ScheduledDateResponse dateResp21 = new ScheduledDateResponse(301, 2, "0 6 * * *");

        when(jobRepository.findAll()).thenReturn(Flux.just(job1, job2));

        when(intervalRepository.findByJobId(1)).thenReturn(Flux.just(interval1));
        when(intervalRepository.findByJobId(2)).thenReturn(Flux.empty());

        when(dateRepository.findByJobId(1)).thenReturn(Flux.just(date11, date12));
        when(dateRepository.findByJobId(2)).thenReturn(Flux.just(date21));

        when(intervalMapper.entityToApi(interval1)).thenReturn(intervalResp1);
        when(dateMapper.entityToApi(date11)).thenReturn(dateResp11);
        when(dateMapper.entityToApi(date12)).thenReturn(dateResp12);
        when(dateMapper.entityToApi(date21)).thenReturn(dateResp21);

        StepVerifier.create(testedObject.findAll())
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(1);
                    assertThat(resp.getName()).isEqualTo("Job-1");
                    assertThat(resp.getScheduledInterval()).isEqualTo(intervalResp1);
                    assertThat(resp.getScheduledDates()).containsExactly(dateResp11, dateResp12);
                })
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(2);
                    assertThat(resp.getName()).isEqualTo("Job-2");
                    assertThat(resp.getScheduledInterval()).isNull();
                    assertThat(resp.getScheduledDates()).containsExactly(dateResp21);
                })
                .verifyComplete();
    }

    @Test
    void find_existingJobId_responseAggregated() {
        int id = 5;

        ScheduledJobEntity job = new ScheduledJobEntity();
        job.setId(id);
        job.setName("MyJob");

        ScheduledIntervalEntity interval = new ScheduledIntervalEntity();
        interval.setId(100);
        interval.setJobId(id);
        interval.setIntervalInMillis(90_000L);

        ScheduledIntervalResponse intervalResp = new ScheduledIntervalResponse(100, 90_000L);

        ScheduledDateEntity date1 = new ScheduledDateEntity();
        date1.setId(200);
        date1.setJobId(id);
        date1.setCronExpression("0 0 * * *");

        ScheduledDateResponse dateResp1 = new ScheduledDateResponse(200, id, "0 0 * * *");

        when(jobRepository.findById(id)).thenReturn(Mono.just(job));

        when(intervalRepository.findByJobId(id)).thenReturn(Flux.just(interval));
        when(intervalMapper.entityToApi(interval)).thenReturn(intervalResp);

        when(dateRepository.findByJobId(id)).thenReturn(Flux.just(date1));
        when(dateMapper.entityToApi(date1)).thenReturn(dateResp1);

        StepVerifier.create(testedObject.find(id))
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(id);
                    assertThat(resp.getName()).isEqualTo("MyJob");
                    assertThat(resp.getScheduledInterval()).isEqualTo(intervalResp);
                    assertThat(resp.getScheduledDates()).containsExactly(dateResp1);
                })
                .verifyComplete();
    }

    @Test
    void find_missingJobId_emptyMonoReturned() {
        int id = 999;
        when(jobRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.find(id))
                .verifyComplete(); // current impl returns Mono.empty, no error
    }

    @Test
    void create_validJob_jobSavedAndEventPublished() {
        ScheduledJobRequest request = new ScheduledJobRequest("NewJob");

        ScheduledJobEntity saved = new ScheduledJobEntity();
        saved.setId(10);
        saved.setName("NewJob");

        when(jobRepository.getKeys()).thenReturn(Flux.empty());
        when(jobRepository.save(any(ScheduledJobEntity.class))).thenReturn(Mono.just(saved));

        when(intervalRepository.findByJobId(10)).thenReturn(Flux.empty());
        when(dateRepository.findByJobId(10)).thenReturn(Flux.empty());

        StepVerifier.create(testedObject.create(request))
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(10);
                    assertThat(resp.getName()).isEqualTo("NewJob");
                    assertThat(resp.getScheduledInterval()).isNull();
                    assertThat(resp.getScheduledDates()).isEmpty();
                })
                .verifyComplete();

        ArgumentCaptor<ScheduledJobAddedEvent> eventCaptor =
                ArgumentCaptor.forClass(ScheduledJobAddedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobEntity()).isEqualTo(saved);
    }

    @Test
    void update_existingJob_jobUpdatedAndEventPublished() {
        int id = 7;
        ScheduledJobRequest request = new ScheduledJobRequest("UpdatedName");

        ScheduledJobEntity existing = new ScheduledJobEntity();
        existing.setId(id);
        existing.setName("OldName");

        ScheduledJobEntity saved = new ScheduledJobEntity();
        saved.setId(id);
        saved.setName("UpdatedName");

        ScheduledIntervalEntity interval = new ScheduledIntervalEntity();
        interval.setId(300);
        interval.setJobId(id);
        interval.setIntervalInMillis(120_000L);

        ScheduledIntervalResponse intervalResp =
                new ScheduledIntervalResponse(300, 120_000L);

        ScheduledDateEntity date = new ScheduledDateEntity();
        date.setId(400);
        date.setJobId(id);
        date.setCronExpression("0 0 * * *");

        ScheduledDateResponse dateResp =
                new ScheduledDateResponse(400, id, "0 0 * * *");

        when(jobRepository.findById(id)).thenReturn(Mono.just(existing));
        when(jobRepository.save(any(ScheduledJobEntity.class))).thenReturn(Mono.just(saved));

        when(intervalRepository.findByJobId(id)).thenReturn(Flux.just(interval));
        when(intervalMapper.entityToApi(interval)).thenReturn(intervalResp);

        when(dateRepository.findByJobId(id)).thenReturn(Flux.just(date));
        when(dateMapper.entityToApi(date)).thenReturn(dateResp);

        StepVerifier.create(testedObject.update(id, request))
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(id);
                    assertThat(resp.getName()).isEqualTo("UpdatedName");
                    assertThat(resp.getScheduledInterval()).isEqualTo(intervalResp);
                    assertThat(resp.getScheduledDates()).containsExactly(dateResp);
                })
                .verifyComplete();

        ArgumentCaptor<ScheduledJobModifiedEvent> eventCaptor =
                ArgumentCaptor.forClass(ScheduledJobModifiedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobEntity()).isEqualTo(saved);
    }

    @Test
    void update_missingJob_entityNotFoundThrown() {
        int id = 123;
        ScheduledJobRequest request = new ScheduledJobRequest("DoesNotMatter");

        when(jobRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.update(id, request))
                .expectError(EntityNotFoundException.class)
                .verify();
    }

    @Test
    void delete_existingJob_deletedAndEventPublished() {
        int id = 11;

        ScheduledJobEntity job = new ScheduledJobEntity();
        job.setId(id);
        job.setName("ToDelete");

        when(jobRepository.findById(id)).thenReturn(Mono.just(job));
        when(jobRepository.delete(job)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.delete(id))
                .verifyComplete();

        ArgumentCaptor<ScheduledJobRemovedEvent> eventCaptor =
                ArgumentCaptor.forClass(ScheduledJobRemovedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobEntity()).isEqualTo(job);
    }

    @Test
    void delete_missingJob_entityNotFoundThrown() {
        int id = 11;
        when(jobRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.delete(id))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(jobRepository, never()).delete(any());
    }
}
