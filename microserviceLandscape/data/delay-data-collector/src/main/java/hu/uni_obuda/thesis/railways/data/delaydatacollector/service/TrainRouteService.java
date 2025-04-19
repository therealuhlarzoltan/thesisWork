package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TrainRouteService {
    Mono<TrainRouteResponse> getTrainRoute(String trainNumber);
    Flux<TrainRouteResponse> getAllTrainRoutes();
    Mono<TrainRouteResponse> createTrainRoute(TrainRouteRequest trainRouteRequest);
    Mono<TrainRouteResponse> updateTrainRoute(TrainRouteRequest trainRouteRequest);
    Mono<Void> deleteTrainRoute(String trainNumber);
}
