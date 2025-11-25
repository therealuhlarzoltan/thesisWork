package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.DelayService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.scheduled.TrainDelayProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelayDataCollectorControllerTest {

    @Mock
    private DelayService delayService;
    @Mock
    private TrainDelayProcessor trainDelayProcessor;

    @InjectMocks
    private DelayDataCollectorController testedObject;

    @Test
    void fetchDelays_invokesProcessorAndCompletes() {
        Mono<Void> result = testedObject.fetchDelays();

        StepVerifier.create(result)
                .verifyComplete();

        verify(trainDelayProcessor, times(1)).processTrainRoutes();
        verifyNoMoreInteractions(delayService, trainDelayProcessor);
    }

    @Test
    void fetchDelay_invokesProcessorWithTrainNumberAndCompletes() {
        String trainNumber = "12345";

        Mono<Void> result = testedObject.fetchDelay(trainNumber);

        StepVerifier.create(result)
                .verifyComplete();

        verify(trainDelayProcessor, times(1)).processTrainRoute(trainNumber);
        verifyNoMoreInteractions(delayService, trainDelayProcessor);
    }
}
