package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.EmmaRailDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Profile("data-source-emma")
@RestController
@RequiredArgsConstructor
public class EmmaRailDataController implements EmmaRailDataCollector {

    private final EmmaRailDataService service;

    @Override
    public Flux<DelayInfo> getDelayInfo(String trainId, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return service.getDelayInfo(trainId, from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date);
    }

    @Override
    public Flux<TrainRouteResponse> planRoute(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return service.planRoute(from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date);
    }
}
