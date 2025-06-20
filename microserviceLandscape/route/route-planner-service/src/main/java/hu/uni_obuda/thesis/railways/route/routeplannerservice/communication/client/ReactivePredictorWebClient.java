package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Primary
@Component
public class ReactivePredictorWebClient implements PredictorWebClient {

    private final WebClient webClient;

    @Value("${app.delay-predictor-service-url}")
    private String baseUrl;
    @Value("${app.delay-predictor-service-departure-uri}")
    private String departureUri;
    @Value("${app.delay-predictor-service-arrival-uri}")
    private String arrivalUri;

    @Override
    public Mono<DelayPredictionResponse> makeArrivalPredictionRequest(DelayPredictionRequest delayPredictionRequest) {
        return webClient.post()
                .uri(baseUrl + arrivalUri)
                .bodyValue(delayPredictionRequest)
                .retrieve()
                .bodyToMono(DelayPredictionResponse.class)
                .onErrorResume(this::handleWebClientException);
    }

    @Override
    public Mono<DelayPredictionResponse> makeDeparturePredictionRequest(DelayPredictionRequest delayPredictionRequest) {
        return webClient.post()
                .uri(baseUrl + departureUri)
                .bodyValue(delayPredictionRequest)
                .retrieve()
                .bodyToMono(DelayPredictionResponse.class)
                .onErrorResume(this::handleWebClientException);
    }

    private Mono<DelayPredictionResponse> handleWebClientException(Throwable throwable) {
        return Mono.error(new RuntimeException("Prediction API call failed", throwable));
    }
}