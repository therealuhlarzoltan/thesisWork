package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RailDataGateway {
    Flux<TrainRouteResponse> getTimetable(String from, String to, LocalDate date);
}
