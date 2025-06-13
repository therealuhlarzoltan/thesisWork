package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
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
                .bodyToMono(DelayPredictionResponse.class);
    }

    @Override
    public Mono<DelayPredictionResponse> makeDeparturePredictionRequest(DelayPredictionRequest delayPredictionRequest) {
        return webClient.post()
                .uri(baseUrl + departureUri)
                .bodyValue(delayPredictionRequest)
                .retrieve()
                .bodyToMono(DelayPredictionResponse.class);
    }
}