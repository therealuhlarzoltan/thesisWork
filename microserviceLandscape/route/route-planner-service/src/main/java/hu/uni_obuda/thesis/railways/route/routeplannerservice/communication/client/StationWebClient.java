package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import reactor.core.publisher.Mono;

public interface StationWebClient {
    Mono<TrainStationResponse> makeStationRequest(String stationCode);
    Mono<TrainRouteResponse> makeTrainRouteRequest(String trainNumber);
}
