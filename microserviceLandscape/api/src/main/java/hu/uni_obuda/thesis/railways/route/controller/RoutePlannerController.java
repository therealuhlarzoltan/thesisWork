package hu.uni_obuda.thesis.railways.route.controller;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface RoutePlannerController {

    @GetMapping("/plan")
    Flux<RouteResponse> planRoute(@RequestParam String from, @RequestParam String to, @RequestParam(required = false) LocalDateTime departureTime, @RequestParam(required = false) LocalDateTime arrivalTime, @RequestParam(required = false) Integer maxChanges);
}
