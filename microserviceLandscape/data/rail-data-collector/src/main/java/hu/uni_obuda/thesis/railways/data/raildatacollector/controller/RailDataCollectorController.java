package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public class RailDataCollectorController implements RailDataCollector {
    @Override
    public Flux<DelayInfo> getDelayInfo(String trainNumber, LocalDate date) {
        return null;
    }
}
