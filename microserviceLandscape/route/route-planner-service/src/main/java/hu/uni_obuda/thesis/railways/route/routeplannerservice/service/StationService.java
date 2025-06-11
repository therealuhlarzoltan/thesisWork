package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import reactor.core.publisher.Mono;

public interface StationService {
    Mono<TrainStationResponse> getStation(String stationCode);
    Mono<TrainRouteResponse> getRoute(String trainNumber);
}
