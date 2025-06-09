package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RailDataCollector {
    @GetMapping("/collect-delay")
    Flux<DelayInfo> getDelayInfo(@RequestParam String trainNumber, @RequestParam String from, @RequestParam String to, @RequestParam LocalDate date);
    @GetMapping("/plan-route")
    Flux<RouteResponse> planRoute(@RequestParam String from, @RequestParam String to, @RequestParam LocalDate date);
}
