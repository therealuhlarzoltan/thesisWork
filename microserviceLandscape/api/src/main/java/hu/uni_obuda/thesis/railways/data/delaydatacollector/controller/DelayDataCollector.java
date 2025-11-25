package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

public interface DelayDataCollector {

    @GetMapping("fetch")
    Mono<Void> fetchDelays();

    @GetMapping("fetch/{trainNumber}")
    Mono<Void> fetchDelay(@PathVariable String trainNumber);
}
