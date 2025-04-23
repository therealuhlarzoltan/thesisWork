package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TrainStationService {
    Flux<TrainStationResponse> getTrainStations();
    Mono<TrainStationResponse> getTrainStationById(String id);
}
