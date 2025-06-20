package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface RoutePlannerService {
    Flux<RouteResponse> planRoute(String from, String to, LocalDateTime departureTime, LocalDateTime arrivalTime, Integer maxChanges);
}
