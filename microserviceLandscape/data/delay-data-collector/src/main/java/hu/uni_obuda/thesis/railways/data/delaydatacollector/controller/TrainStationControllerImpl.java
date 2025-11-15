package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.GeocodingService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.domain.TrainStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RequestMapping("/train-stations")
@RestController
public class TrainStationControllerImpl implements TrainStationController {

    private final TrainStationService service;
    private final GeocodingService geocodingService;

    @Override
    public Mono<TrainStationResponse> createTrainStation(TrainStationRequest trainStationRequest) {
        return null;
    }

    @Override
    public Mono<TrainStationResponse> updateTrainStation(TrainStationRequest trainStationRequest) {
        return null;
    }

    @Override
    public Flux<TrainStationResponse> getTrainStations(String stationCode) {
        if (stationCode == null) {
            return service.getTrainStations();
        } else {
            return Flux.from(service.getTrainStationById(stationCode));
        }
    }

    @Override
    public Mono<Void> fetchGeolocationForTrainStation(String trainStationCode) {
        return service.getTrainStationById(trainStationCode).flatMap(entity -> {
           return geocodingService.fetchCoordinatesForStation(entity.getStationCode(), false);
        });
    }

    @Override
    public Mono<Void> fetchGeolocationForAllTrainStations() {
        return service.getTrainStations().flatMap(entity -> {
            return geocodingService.fetchCoordinatesForStation(entity.getStationCode(), false);
        }).then();
    }

    @Override
    public Mono<Void> deleteTrainStation(String stationName) {
        return null;
    }
}
