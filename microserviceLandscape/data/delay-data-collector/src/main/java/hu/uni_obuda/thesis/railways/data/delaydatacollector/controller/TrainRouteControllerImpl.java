package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.domain.TrainRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RequestMapping("/train-routes")
@RestController
public class TrainRouteControllerImpl implements TrainRouteController {

    private final TrainRouteService service;

    @Override
    public Mono<TrainRouteResponse> createTrainRoute(@Valid @RequestBody TrainRouteRequest trainRouteRequest) {
        return service.createTrainRoute(trainRouteRequest);
    }

    @Override
    public Mono<TrainRouteResponse> updateTrainRoute(@Valid @RequestBody TrainRouteRequest trainRouteRequest) {
        return service.updateTrainRoute(trainRouteRequest);
    }

    @Override
    public Flux<TrainRouteResponse> getTrainRoute(@RequestParam(required = false) String trainNumber) {
        if (trainNumber == null) {
            return service.getAllTrainRoutes();
        } else {
            return Flux.from(service.getTrainRoute(trainNumber));
        }
    }

    @Override
    public Mono<Void> deleteTrainRoute(@RequestParam(required = true) String trainNumber) {
        return service.deleteTrainRoute(trainNumber);
    }
}
