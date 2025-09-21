package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.RailDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class RailDataCollectorController implements RailDataCollector {

    private final RailDataService service;

    @Override
    public Flux<DelayInfo> getDelayInfo(String trainNumber, String from, String to, LocalDate date) {
        return service.getDelayInfo(trainNumber, from, to, date);
    }

    @Override
    public Flux<DelayInfo> getDelayInfo(String trainId, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return service.getDelayInfo(trainId, from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date);
    }

    @Override
    public Flux<TrainRouteResponse> planRoute(String from, String to, LocalDate date) {
        return service.planRoute(from, to, date);
    }

    @Override
    public Flux<TrainRouteResponse> planRoute(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return service.planRoute(from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date);
    }
}
