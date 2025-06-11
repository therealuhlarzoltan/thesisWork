package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.PredictorGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Primary
@Service
public class ReactiveHttpPredictionService implements PredictionService {

    private final PredictorGateway predictorGateway;

    public ReactiveHttpPredictionService(@Qualifier("reactivePredictorGateway") PredictorGateway predictorGateway) {
        this.predictorGateway = predictorGateway;
    }

    @Override
    public Mono<DelayPredictionResponse> predictArrivalDelay(DelayPredictionRequest request) {
        return predictorGateway.getArrivalDelay(request);
    }

    @Override
    public Mono<DelayPredictionResponse> predictDepartureDelay(DelayPredictionRequest request) {
        return predictorGateway.getDepartureDelay(request);
    }
}
