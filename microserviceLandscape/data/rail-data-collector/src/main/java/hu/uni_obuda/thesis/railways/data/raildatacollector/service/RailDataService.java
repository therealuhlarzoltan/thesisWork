package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RailDataService {
    Flux<DelayInfo> getDelayInfo(String trainNumber, String from, String to, LocalDate date);

    Flux<TrainRouteResponse> planRoute(String from, String to, LocalDate date);
}
