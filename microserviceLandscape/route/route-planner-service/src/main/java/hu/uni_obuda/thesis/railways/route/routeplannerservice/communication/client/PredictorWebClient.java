package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import reactor.core.publisher.Mono;

public interface PredictorWebClient {
    Mono<DelayPredictionResponse> makeArrivalPredictionRequest(DelayPredictionRequest delayPredictionRequest);
    Mono<DelayPredictionResponse> makeDeparturePredictionRequest(DelayPredictionRequest delayPredictionRequest);
}
