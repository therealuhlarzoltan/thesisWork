package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.common;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.PredictorGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveHttpPredictionServiceTest {

    @Mock
    private PredictorGateway predictorGateway;

    @InjectMocks
    private ReactiveHttpPredictionService testedObject;

    @Test
    void predictArrivalDelay_callsGatewayGetArrivalDelay() {
        DelayPredictionRequest request = mock(DelayPredictionRequest.class);
        DelayPredictionResponse response = mock(DelayPredictionResponse.class);

        when(predictorGateway.getArrivalDelay(request)).thenReturn(Mono.just(response));

        StepVerifier.create(testedObject.predictArrivalDelay(request))
                .expectNext(response)
                .verifyComplete();

        verify(predictorGateway).getArrivalDelay(request);
        verify(predictorGateway, never()).getDepartureDelay(any());
        verifyNoMoreInteractions(predictorGateway);
    }

    @Test
    void predictDepartureDelay_callsGatewayGetDepartureDelay() {
        DelayPredictionRequest request = mock(DelayPredictionRequest.class);
        DelayPredictionResponse response = mock(DelayPredictionResponse.class);

        when(predictorGateway.getDepartureDelay(request)).thenReturn(Mono.just(response));

        StepVerifier.create(testedObject.predictDepartureDelay(request))
                .expectNext(response)
                .verifyComplete();

        verify(predictorGateway).getDepartureDelay(request);
        verify(predictorGateway, never()).getArrivalDelay(any());
        verifyNoMoreInteractions(predictorGateway);
    }
}
