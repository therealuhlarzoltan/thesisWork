package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.PredictorWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Primary
@Component
public class ReactivePredictorGateway implements PredictorGateway {

    private final PredictorWebClient webClient;

    public ReactivePredictorGateway(@Qualifier("reactivePredictorWebClient") PredictorWebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<DelayPredictionResponse> getArrivalDelay(DelayPredictionRequest request) {
        return webClient.makeArrivalPredictionRequest(request);
    }

    @Override
    public Mono<DelayPredictionResponse> getDepartureDelay(DelayPredictionRequest request) {
        return webClient.makeDeparturePredictionRequest(request);
    }
}
