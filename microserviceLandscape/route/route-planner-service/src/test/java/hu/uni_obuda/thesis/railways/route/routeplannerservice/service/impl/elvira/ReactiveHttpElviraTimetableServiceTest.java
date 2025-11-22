package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.elvira;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.ElviraRailDataGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveHttpElviraTimetableServiceTest {

    @Mock
    private ElviraRailDataGateway railDataGateway;

    @InjectMocks
    private ReactiveHttpElviraTimetableService testedObject;

    @Test
    void getTimetable_callsGatewayAndReturnsFlux() {
        String from = "BUDAPEST";
        String to = "GYOR";
        LocalDate date = LocalDate.of(2025, 1, 1);

        TrainRouteResponse route1 = TrainRouteResponse.builder().build();
        TrainRouteResponse route2 = TrainRouteResponse.builder().build();

        when(railDataGateway.getTimetable(from, to, date))
                .thenReturn(Flux.just(route1, route2));

        StepVerifier.create(testedObject.getTimetable(from, to, date))
                .expectNext(route1)
                .expectNext(route2)
                .verifyComplete();

        verify(railDataGateway).getTimetable(from, to, date);
        verifyNoMoreInteractions(railDataGateway);
    }
}
