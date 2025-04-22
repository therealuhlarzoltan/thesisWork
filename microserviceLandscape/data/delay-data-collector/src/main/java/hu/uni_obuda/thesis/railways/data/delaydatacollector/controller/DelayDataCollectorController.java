package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.DelayService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.scheduled.TrainDelayProcessor;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController("delays")
public class DelayDataCollectorController implements DelayDataCollector {

    private final DelayService delayService;
    private final TrainDelayProcessor trainDelayProcessor;

    @GetMapping("get")
    @Override
    public Flux<DelayInfo> getTrainDelays() {
        return delayService.getTrainDelays();
    }

    @GetMapping("fetch")
    @Override
    public Mono<Void> fetchDelays() {
        return Mono.fromRunnable(trainDelayProcessor::processTrainRoutes);
    }
}
