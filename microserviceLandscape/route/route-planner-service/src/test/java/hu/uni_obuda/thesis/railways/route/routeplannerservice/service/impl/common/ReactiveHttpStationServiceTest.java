package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.common;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.StationDataGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveHttpStationServiceTest {

    @Mock
    private StationDataGateway stationDataGateway;

    @InjectMocks
    private ReactiveHttpStationService testedObject;

    @Test
    void getStation_callsGatewayGetTrainStation() {
        String stationCode = "BUD";
        TrainStationResponse response = TrainStationResponse.builder()
                .stationCode(stationCode)
                .latitude(47.5)
                .longitude(19.0)
                .build();

        when(stationDataGateway.getTrainStation(stationCode)).thenReturn(Mono.just(response));

        StepVerifier.create(testedObject.getStation(stationCode))
                .expectNext(response)
                .verifyComplete();

        verify(stationDataGateway).getTrainStation(stationCode);
        verify(stationDataGateway, never()).getTrainRoute(anyString());
        verifyNoMoreInteractions(stationDataGateway);
    }

    @Test
    void getRoute_callsGatewayGetTrainRoute() {
        String trainNumber = "1234";
        TrainRouteResponse response = new TrainRouteResponse();

        when(stationDataGateway.getTrainRoute(trainNumber)).thenReturn(Mono.just(response));

        StepVerifier.create(testedObject.getRoute(trainNumber))
                .expectNext(response)
                .verifyComplete();

        verify(stationDataGateway).getTrainRoute(trainNumber);
        verify(stationDataGateway, never()).getTrainStation(anyString());
        verifyNoMoreInteractions(stationDataGateway);
    }
}
