package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface ElviraRailDataCollector {
    @GetMapping("/collect-delay")
    Flux<DelayInfo> getDelayInfo(@RequestParam String trainNumber, @RequestParam String from, @RequestParam String to, @RequestParam LocalDate date);
    @GetMapping("/plan-route")
    Flux<TrainRouteResponse> planRoute(@RequestParam String from, @RequestParam String to, @RequestParam LocalDate date);
}
