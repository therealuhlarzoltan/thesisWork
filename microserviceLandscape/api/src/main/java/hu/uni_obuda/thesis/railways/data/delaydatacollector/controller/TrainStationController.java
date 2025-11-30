package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TrainStationController {
    @PostMapping
    Mono<TrainStationResponse> createTrainStation(@RequestBody @Valid TrainStationRequest trainStationRequest);
    @PutMapping
    Mono<TrainStationResponse> updateTrainStation(@RequestBody @Valid TrainStationRequest trainStationRequest);
    @GetMapping
    Flux<TrainStationResponse> getTrainStations(@RequestParam(required = false) String stationCode);
    @PatchMapping("fetch/{trainStationCode}")
    Mono<Void> fetchGeolocationForTrainStation(@PathVariable String trainStationCode, @RequestParam boolean force);
    @PatchMapping("fetch/all")
    Mono<Void> fetchGeolocationForAllTrainStations(@RequestParam boolean force);
    @DeleteMapping
    Mono<Void> deleteTrainStation(@RequestParam String stationName);
}
