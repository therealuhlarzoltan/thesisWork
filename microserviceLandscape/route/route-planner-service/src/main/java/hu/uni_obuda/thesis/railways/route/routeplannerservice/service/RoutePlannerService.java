package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;

import java.time.LocalDateTime;

public interface RoutePlannerService {
    RouteResponse planRoute(String from, String to, LocalDateTime dateTime);
}
