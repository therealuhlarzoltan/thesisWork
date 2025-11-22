package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.emma;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.EmmaRailDataGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveHttpEmmaTimetableServiceTest {

    @Mock
    private EmmaRailDataGateway emmaRailDataGateway;

    @InjectMocks
    private ReactiveHttpEmmaTimetableService testedObject;

    @Test
    void getTimetable_callsGatewayAndReturnsFlux() {
        String from = "BUDAPEST";
        double fromLat = 47.5;
        double fromLon = 19.0;
        String to = "GYOR";
        double toLat = 47.7;
        double toLon = 17.6;
        LocalDate date = LocalDate.of(2025, 1, 1);

        TrainRouteResponse route1 = TrainRouteResponse.builder().build();
        TrainRouteResponse route2 = TrainRouteResponse.builder().build();

        when(emmaRailDataGateway.getTimetable(from, fromLat, fromLon, to, toLat, toLon, date))
                .thenReturn(Flux.just(route1, route2));

        StepVerifier.create(testedObject.getTimetable(from, fromLat, fromLon, to, toLat, toLon, date))
                .expectNext(route1)
                .expectNext(route2)
                .verifyComplete();

        verify(emmaRailDataGateway).getTimetable(from, fromLat, fromLon, to, toLat, toLon, date);
        verifyNoMoreInteractions(emmaRailDataGateway);
    }
}
