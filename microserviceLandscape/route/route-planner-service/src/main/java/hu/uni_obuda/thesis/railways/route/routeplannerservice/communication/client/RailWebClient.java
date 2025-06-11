package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RailWebClient {
    Flux<TrainRouteResponse> makeRouteRequest(String from, String to, LocalDate date);
}
