package hu.uni_obuda.thesis.railways.route.routeplannerservice.controller;

import hu.uni_obuda.thesis.railways.route.controller.RoutePlannerController;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.RoutePlannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/route")
@RestController
public class RoutePlannerControllerImpl implements RoutePlannerController {

    private final RoutePlannerService routePlannerService;

    @Override
    public Flux<RouteResponse> planRoute(String from, String to, LocalDateTime departureTime, LocalDateTime arrivalTime, Integer maxChanges) {
        return routePlannerService.planRoute(from, to, departureTime, arrivalTime, maxChanges);
    }
}
