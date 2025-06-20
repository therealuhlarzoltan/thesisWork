package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.PredictorWebClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
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
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public ReactivePredictorGateway(@Qualifier("reactivePredictorWebClient") PredictorWebClient webClient,
                                    CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.webClient = webClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public Mono<DelayPredictionResponse> getArrivalDelay(DelayPredictionRequest request) {
        return webClient.makeArrivalPredictionRequest(request)
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getArrivalDelay")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getArrivalDelay")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException callNotPermittedException) {
                        log.error("Circuit breaker is open", callNotPermittedException);
                    }
                    log.warn("Returning default arrival delay prediction response because of exception", throwable);
                    return Mono.just(DelayPredictionResponse.builder()
                            .stationCode(request.getStationCode())
                            .trainNumber(request.getTrainNumber())
                            .predictedDelay(0d).build());
                });
    }

    @Override
    public Mono<DelayPredictionResponse> getDepartureDelay(DelayPredictionRequest request) {
        return webClient.makeDeparturePredictionRequest(request)
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getDepartureDelay")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getDepartureDelay")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException callNotPermittedException) {
                        log.error("Circuit breaker is open", callNotPermittedException);
                    }
                    log.warn("Returning default departure delay prediction response because of exception", throwable);
                    return Mono.just(DelayPredictionResponse.builder()
                            .stationCode(request.getStationCode())
                            .trainNumber(request.getTrainNumber())
                            .predictedDelay(0d).build());
                });
    }
}
