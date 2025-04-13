package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TrainRouteController {
    @PostMapping
    Mono<TrainRouteResponse> createTrainRoute(@RequestBody TrainRouteRequest trainRouteRequest);
    @PutMapping
    Mono<TrainRouteResponse> updateTrainRoute(@RequestParam(required = true) int routeId, @RequestBody TrainRouteRequest trainRouteRequest);
    @GetMapping
    Flux<TrainRouteResponse> getTrainRoute(@RequestParam(required = false) Integer routeId);
    @DeleteMapping
    Mono<Void> deleteTrainRoute(@RequestParam(required = true) int routeId);
}
