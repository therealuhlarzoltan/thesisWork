package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.RailDataService;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
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
    public Flux<RouteResponse> planRoute(String from, String to, LocalDate date) {
        return service.planRoute(from, to, date);
    }
}
