package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DelayDataCollector {
    Flux<DelayInfo> getTrainDelays();
    Mono<Void> fetchDelays();
}
