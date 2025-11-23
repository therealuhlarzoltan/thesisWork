package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledDateService;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledIntervalService;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.scheduler.ScheduledJobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerControllerImplTest {

    @Mock
    private ScheduledJobService jobService;
    @Mock
    private ScheduledDateService dateService;
    @Mock
    private ScheduledIntervalService intervalService;

    @InjectMocks
    private SchedulerControllerImpl testedObject;

    @Test
    void getAllJobs_delegatesToJobService() {
        Flux<ScheduledJobResponse> serviceResult = Flux.empty();
        when(jobService.findAll()).thenReturn(serviceResult);

        Flux<ScheduledJobResponse> result = testedObject.getAllJobs();

        assertThat(result).isSameAs(serviceResult);
        verify(jobService).findAll();
        verifyNoMoreInteractions(jobService);
    }

    @Test
    void getJob_delegatesToJobService() {
        int id = 42;
        Mono<ScheduledJobResponse> serviceResult = Mono.empty();
        when(jobService.find(id)).thenReturn(serviceResult);

        Mono<ScheduledJobResponse> result = testedObject.getJob(id);

        assertThat(result).isSameAs(serviceResult);
        verify(jobService).find(id);
        verifyNoMoreInteractions(jobService);
    }

    @Test
    void createJob_delegatesToJobService() {
        ScheduledJobRequest request = new ScheduledJobRequest();
        Mono<ScheduledJobResponse> serviceResult = Mono.empty();
        when(jobService.create(request)).thenReturn(serviceResult);

        Mono<ScheduledJobResponse> result = testedObject.createJob(request);

        assertThat(result).isSameAs(serviceResult);
        verify(jobService).create(request);
        verifyNoMoreInteractions(jobService);
    }

    @Test
    void updateJob_delegatesToJobService() {
        int id = 13;
        ScheduledJobRequest request = new ScheduledJobRequest();
        Mono<ScheduledJobResponse> serviceResult = Mono.empty();
        when(jobService.update(id, request)).thenReturn(serviceResult);

        Mono<ScheduledJobResponse> result = testedObject.updateJob(id, request);

        assertThat(result).isSameAs(serviceResult);
        verify(jobService).update(id, request);
        verifyNoMoreInteractions(jobService);
    }

    @Test
    void deleteJob_delegatesToJobService() {
        int id = 7;
        Mono<Void> serviceResult = Mono.empty();
        when(jobService.delete(id)).thenReturn(serviceResult);

        Mono<Void> result = testedObject.deleteJob(id);

        assertThat(result).isSameAs(serviceResult);
        verify(jobService).delete(id);
        verifyNoMoreInteractions(jobService);
    }

    @Test
    void getAllDates_delegatesToDateService() {
        Flux<ScheduledDateResponse> serviceResult = Flux.empty();
        when(dateService.findAll()).thenReturn(serviceResult);

        Flux<ScheduledDateResponse> result = testedObject.getAllDates();

        assertThat(result).isSameAs(serviceResult);
        verify(dateService).findAll();
        verifyNoMoreInteractions(dateService);
    }

    @Test
    void getDate_delegatesToDateService() {
        int id = 21;
        Mono<ScheduledDateResponse> serviceResult = Mono.empty();
        when(dateService.find(id)).thenReturn(serviceResult);

        Mono<ScheduledDateResponse> result = testedObject.getDate(id);

        assertThat(result).isSameAs(serviceResult);
        verify(dateService).find(id);
        verifyNoMoreInteractions(dateService);
    }

    @Test
    void createDate_validatesAndDelegatesToDateService() {
        ScheduledDateRequest request = new ScheduledDateRequest();
        request.setCronExpression("0 0 12 * * *");

        ScheduledDateResponse response = new ScheduledDateResponse();
        when(dateService.create(request)).thenReturn(Mono.just(response));

        Mono<ScheduledDateResponse> result = testedObject.createDate(request);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        verify(dateService).create(request);
        verifyNoMoreInteractions(dateService);
    }

    @Test
    void updateDate_validatesAndDelegatesToDateService() {
        int id = 9;
        ScheduledDateRequest request = new ScheduledDateRequest();
        request.setCronExpression("0 0 12 * * *");

        ScheduledDateResponse response = new ScheduledDateResponse();
        when(dateService.update(id, request)).thenReturn(Mono.just(response));

        Mono<ScheduledDateResponse> result = testedObject.updateDate(id, request);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        verify(dateService).update(id, request);
        verifyNoMoreInteractions(dateService);
    }

    @Test
    void deleteDate_delegatesToDateService() {
        int id = 5;
        Mono<Void> serviceResult = Mono.empty();
        when(dateService.delete(id)).thenReturn(serviceResult);

        Mono<Void> result = testedObject.deleteDate(id);

        assertThat(result).isSameAs(serviceResult);
        verify(dateService).delete(id);
        verifyNoMoreInteractions(dateService);
    }

    @Test
    void getAllIntervals_delegatesToIntervalService() {
        Flux<ScheduledIntervalResponse> serviceResult = Flux.empty();
        when(intervalService.findAll()).thenReturn(serviceResult);

        Flux<ScheduledIntervalResponse> result = testedObject.getAllIntervals();

        assertThat(result).isSameAs(serviceResult);
        verify(intervalService).findAll();
        verifyNoMoreInteractions(intervalService);
    }

    @Test
    void getInterval_delegatesToIntervalService() {
        int id = 11;
        Mono<ScheduledIntervalResponse> serviceResult = Mono.empty();
        when(intervalService.find(id)).thenReturn(serviceResult);

        Mono<ScheduledIntervalResponse> result = testedObject.getInterval(id);

        assertThat(result).isSameAs(serviceResult);
        verify(intervalService).find(id);
        verifyNoMoreInteractions(intervalService);
    }

    @Test
    void createInterval_delegatesToIntervalService() {
        ScheduledIntervalRequest request = new ScheduledIntervalRequest();
        Mono<ScheduledIntervalResponse> serviceResult = Mono.empty();
        when(intervalService.create(request)).thenReturn(serviceResult);

        Mono<ScheduledIntervalResponse> result = testedObject.createInterval(request);

        assertThat(result).isSameAs(serviceResult);
        verify(intervalService).create(request);
        verifyNoMoreInteractions(intervalService);
    }

    @Test
    void updateInterval_delegatesToIntervalService() {
        int id = 4;
        ScheduledIntervalRequest request = new ScheduledIntervalRequest();
        Mono<ScheduledIntervalResponse> serviceResult = Mono.empty();
        when(intervalService.update(id, request)).thenReturn(serviceResult);

        Mono<ScheduledIntervalResponse> result = testedObject.updateInterval(id, request);

        assertThat(result).isSameAs(serviceResult);
        verify(intervalService).update(id, request);
        verifyNoMoreInteractions(intervalService);
    }

    @Test
    void deleteInterval_delegatesToIntervalService() {
        int id = 2;
        Mono<Void> serviceResult = Mono.empty();
        when(intervalService.delete(id)).thenReturn(serviceResult);

        Mono<Void> result = testedObject.deleteInterval(id);

        assertThat(result).isSameAs(serviceResult);
        verify(intervalService).delete(id);
        verifyNoMoreInteractions(intervalService);
    }
}
