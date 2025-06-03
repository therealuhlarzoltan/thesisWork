package hu.uni_obuda.thesis.railways.route.routeplannerservice.controller;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.RoutePlannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/route")
@RestController
public class RoutePlannerControllerImpl {

    private final RoutePlannerService routePlannerService;

    @Override
    public RouteResponse planRoute(String from, String to, LocalDateTime dateTime) {
        return routePlannerService.planRoute(from, to, dateTime);
    }
}
