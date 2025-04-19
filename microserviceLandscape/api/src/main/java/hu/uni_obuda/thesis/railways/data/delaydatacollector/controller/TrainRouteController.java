package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TrainRouteController {
    @PostMapping
    Mono<TrainRouteResponse> createTrainRoute(@RequestBody @Valid TrainRouteRequest trainRouteRequest);
    @PutMapping
    Mono<TrainRouteResponse> updateTrainRoute(@RequestBody @Valid TrainRouteRequest trainRouteRequest);
    @GetMapping
    Flux<TrainRouteResponse> getTrainRoute(@RequestParam(required = false) String trainNumber);
    @DeleteMapping
    Mono<Void> deleteTrainRoute(@RequestParam(required = true) String trainNumber);
}
