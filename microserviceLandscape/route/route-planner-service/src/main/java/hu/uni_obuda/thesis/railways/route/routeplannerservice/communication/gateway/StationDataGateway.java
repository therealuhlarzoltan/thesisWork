package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import reactor.core.publisher.Mono;

public interface StationDataGateway {
    Mono<TrainRouteResponse> getTrainRoute(String trainNumber);
    Mono<TrainStationResponse> getTrainStation(String stationCode);
}
