package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.TrainRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController("train-routes")
public class TrainRouteControllerImpl implements TrainRouteController {

    private final TrainRouteService service;

    @Override
    public Mono<TrainRouteResponse> createTrainRoute(@Valid TrainRouteRequest trainRouteRequest) {
        return service.createTrainRoute(trainRouteRequest);
    }

    @Override
    public Mono<TrainRouteResponse> updateTrainRoute(@Valid TrainRouteRequest trainRouteRequest) {
        return service.updateTrainRoute(trainRouteRequest);
    }

    @Override
    public Flux<TrainRouteResponse> getTrainRoute(String trainNumber) {
        if (trainNumber == null) {
            return service.getAllTrainRoutes();
        } else {
            return Flux.from(service.getTrainRoute(trainNumber));
        }
    }

    @Override
    public Mono<Void> deleteTrainRoute(String trainNumber) {
        return service.deleteTrainRoute(trainNumber);
    }
}
