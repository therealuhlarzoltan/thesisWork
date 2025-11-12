package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface ElviraTimetableService {
    Flux<TrainRouteResponse> getTimetable(String from, String to, LocalDate date);
}
