package hu.uni_obuda.thesis.railways.route.routeplannerservice.controller;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.RoutePlannerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutePlannerControllerTest {

    @Mock
    private RoutePlannerService routePlannerService;

    @InjectMocks
    private RoutePlannerControllerImpl testedObject;

    @Test
    void planRoute_callsServiceAndReturnsFlux() {
        String from = "BUDAPEST";
        String to = "GYOR";
        LocalDateTime departureTime = LocalDateTime.of(2025, 1, 1, 8, 0);
        LocalDateTime arrivalTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        Integer maxChanges = 2;

        RouteResponse route1 = RouteResponse.builder().build();
        RouteResponse route2 = RouteResponse.builder().build();

        when(routePlannerService.planRoute(from, to, departureTime, arrivalTime, maxChanges))
                .thenReturn(Flux.just(route1, route2));

        StepVerifier.create(testedObject.planRoute(from, to, departureTime, arrivalTime, maxChanges))
                .expectNext(route1)
                .expectNext(route2)
                .verifyComplete();

        verify(routePlannerService).planRoute(from, to, departureTime, arrivalTime, maxChanges);
        verifyNoMoreInteractions(routePlannerService);
    }
}
