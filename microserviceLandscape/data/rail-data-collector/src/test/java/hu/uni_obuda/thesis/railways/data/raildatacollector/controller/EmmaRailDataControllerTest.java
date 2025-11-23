package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.data.EmmaRailDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmmaRailDataControllerTest {

    @Mock
    private EmmaRailDataService service;

    @InjectMocks
    private EmmaRailDataController testedObject;

    @Test
    void getDelayInfo_delegatesToServiceWithSameArgs() {
        String trainId = "IC123";
        String from = "BUDAPEST";
        double fromLatitude = 47.4979;
        double fromLongitude = 19.0402;
        String to = "GYOR";
        double toLatitude = 47.6875;
        double toLongitude = 17.6504;
        LocalDate date = LocalDate.of(2025, 1, 1);

        Flux<DelayInfo> serviceResult = Flux.empty();
        when(service.getDelayInfo(
                trainId,
                from,
                fromLatitude,
                fromLongitude,
                to,
                toLatitude,
                toLongitude,
                date
        )).thenReturn(serviceResult);

        Flux<DelayInfo> result = testedObject.getDelayInfo(
                trainId,
                from,
                fromLatitude,
                fromLongitude,
                to,
                toLatitude,
                toLongitude,
                date
        );

        assertThat(result).isSameAs(serviceResult);
        verify(service, times(1)).getDelayInfo(
                trainId,
                from,
                fromLatitude,
                fromLongitude,
                to,
                toLatitude,
                toLongitude,
                date
        );
        verifyNoMoreInteractions(service);
    }

    @Test
    void planRoute_delegatesToServiceWithSameArgs() {
        String from = "BUDAPEST";
        double fromLatitude = 47.4979;
        double fromLongitude = 19.0402;
        String to = "DEBRECEN";
        double toLatitude = 47.5316;
        double toLongitude = 21.6273;
        LocalDate date = LocalDate.of(2025, 2, 2);

        Flux<TrainRouteResponse> serviceResult = Flux.empty();
        when(service.planRoute(
                from,
                fromLatitude,
                fromLongitude,
                to,
                toLatitude,
                toLongitude,
                date
        )).thenReturn(serviceResult);

        Flux<TrainRouteResponse> result = testedObject.planRoute(
                from,
                fromLatitude,
                fromLongitude,
                to,
                toLatitude,
                toLongitude,
                date
        );

        assertThat(result).isSameAs(serviceResult);
        verify(service, times(1)).planRoute(
                from,
                fromLatitude,
                fromLongitude,
                to,
                toLatitude,
                toLongitude,
                date
        );
        verifyNoMoreInteractions(service);
    }
}
