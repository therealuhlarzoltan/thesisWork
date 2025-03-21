package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RailDataCollector {
    @GetMapping("/collect-delay")
    Flux<DelayInfo> getDelayInfo(@RequestParam String trainNumber, @RequestParam LocalDate date);
}
