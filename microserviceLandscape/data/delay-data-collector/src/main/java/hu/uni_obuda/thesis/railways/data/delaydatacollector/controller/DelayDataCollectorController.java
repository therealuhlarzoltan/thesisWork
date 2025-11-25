package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.DelayService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.scheduled.TrainDelayProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController("delays")
public class DelayDataCollectorController implements DelayDataCollector {

    private final DelayService delayService;
    private final TrainDelayProcessor trainDelayProcessor;

    @Override
    public Mono<Void> fetchDelays() {
        return Mono.fromRunnable(trainDelayProcessor::processTrainRoutes);
    }

    @Override
    public Mono<Void> fetchDelay(@PathVariable String trainNumber) {
        return Mono.fromRunnable(() -> trainDelayProcessor.processTrainRoute(trainNumber));
    }
}
