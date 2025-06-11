package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import reactor.core.publisher.Mono;

public interface PredictionService {
    Mono<DelayPredictionResponse> predictArrivalDelay(DelayPredictionRequest request);
    Mono<DelayPredictionResponse> predictDepartureDelay(DelayPredictionRequest request);
}
