package hu.uni_obuda.thesis.railways.route.controller;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

public interface RoutePlannerController {

    @GetMapping("/plan")
    public RouteResponse planRoute(@RequestParam String from, @RequestParam String to, @RequestParam LocalDateTime dateTime);
}
