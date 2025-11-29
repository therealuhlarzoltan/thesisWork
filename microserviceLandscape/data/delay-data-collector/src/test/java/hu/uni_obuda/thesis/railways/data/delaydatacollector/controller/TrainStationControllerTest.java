package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.GeocodingService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.domain.TrainStationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainStationControllerTest {

    @Mock
    private TrainStationService service;
    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private TrainStationControllerImpl testedObject;

    @Test
    void getTrainStations_withoutStationCode_callsGetTrainStationsOnService() {
        TrainStationResponse station1 = mock(TrainStationResponse.class);
        TrainStationResponse station2 = mock(TrainStationResponse.class);
        when(service.getTrainStations()).thenReturn(Flux.just(station1, station2));

        Flux<TrainStationResponse> result = testedObject.getTrainStations(null);

        StepVerifier.create(result)
                .expectNext(station1, station2)
                .verifyComplete();

        verify(service, times(1)).getTrainStations();
        verifyNoMoreInteractions(service, geocodingService);
    }

    @Test
    void getTrainStations_withStationCode_callsGetTrainStationByIdOnService() {
        String code = "ST001";
        TrainStationResponse station = mock(TrainStationResponse.class);
        when(service.getTrainStationById(code)).thenReturn(Mono.just(station));

        Flux<TrainStationResponse> result = testedObject.getTrainStations(code);

        StepVerifier.create(result)
                .expectNext(station)
                .verifyComplete();

        verify(service, times(1)).getTrainStationById(code);
        verifyNoMoreInteractions(service, geocodingService);
    }

    @Test
    void fetchGeolocationForTrainStation_fetchesStationThenCallsGeocodingService() {
        String code = "ST002";
        TrainStationResponse station = mock(TrainStationResponse.class);
        when(station.getStationCode()).thenReturn(code);

        when(service.getTrainStationById(code)).thenReturn(Mono.just(station));
        when(geocodingService.fetchCoordinatesForStation(code, false))
                .thenReturn(Mono.empty());

        Mono<Void> result = testedObject.fetchGeolocationForTrainStation(code);

        StepVerifier.create(result)
                .verifyComplete();

        verify(service, times(1)).getTrainStationById(code);
        verify(geocodingService, times(1))
                .fetchCoordinatesForStation(code, false);
        verifyNoMoreInteractions(service, geocodingService);
    }

    @Test
    void fetchGeolocationForAllTrainStations_callsGeocodingForEachStation() {
        TrainStationResponse station1 = mock(TrainStationResponse.class);
        TrainStationResponse station2 = mock(TrainStationResponse.class);

        when(station1.getStationCode()).thenReturn("ST001");
        when(station2.getStationCode()).thenReturn("ST002");

        when(service.getTrainStations()).thenReturn(Flux.just(station1, station2));
        when(geocodingService.fetchCoordinatesForStation(anyString(), eq(false)))
                .thenReturn(Mono.empty());

        Mono<Void> result = testedObject.fetchGeolocationForAllTrainStations();

        StepVerifier.create(result)
                .verifyComplete();

        verify(service, times(1)).getTrainStations();
        verify(geocodingService, times(1))
                .fetchCoordinatesForStation("ST001", false);
        verify(geocodingService, times(1))
                .fetchCoordinatesForStation("ST002", false);
        verifyNoMoreInteractions(service, geocodingService);
    }
}
