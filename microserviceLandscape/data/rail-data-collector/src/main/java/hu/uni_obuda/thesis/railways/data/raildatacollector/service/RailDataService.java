package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RailDataService {
    Flux<DelayInfo> getDelayInfo(String trainNumber, LocalDate date);
}
