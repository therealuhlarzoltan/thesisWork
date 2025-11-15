package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface EmmaRailDataCollector {
    @GetMapping("/collect")
    Flux<DelayInfo> getDelayInfo(@RequestParam String trainId, @RequestParam String from, @RequestParam double fromLatitude,
                                 @RequestParam double fromLongitude, @RequestParam String to, @RequestParam double toLatitude,
                                 @RequestParam double toLongitude, @RequestParam LocalDate date);
    @GetMapping("/plan-route")
    Flux<TrainRouteResponse> planRoute(@RequestParam String from, @RequestParam double fromLatitude, @RequestParam double fromLongitude,
                                       @RequestParam String to, @RequestParam double toLatitude, @RequestParam double toLongitude,
                                       @RequestParam LocalDate date);
}
