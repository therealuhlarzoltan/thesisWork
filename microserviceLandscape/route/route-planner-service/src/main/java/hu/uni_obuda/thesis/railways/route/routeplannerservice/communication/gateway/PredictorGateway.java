package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import reactor.core.publisher.Mono;

public interface PredictorGateway {
    Mono<DelayPredictionResponse> getArrivalDelay(DelayPredictionRequest request);
    Mono<DelayPredictionResponse> getDepartureDelay(DelayPredictionRequest request);
}
