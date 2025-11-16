package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.PredictorWebClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
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
    private final RateLimiterRegistry rateLimiterRegistry;

    public ReactivePredictorGateway(@Qualifier("reactivePredictorWebClient") PredictorWebClient webClient,
                                    CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry,
                                    RateLimiterRegistry rateLimiterRegistry) {
        this.webClient = webClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Mono<DelayPredictionResponse> getArrivalDelay(DelayPredictionRequest request) {
        return webClient.makeArrivalPredictionRequest(request)
                .transform(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getArrivalDelay")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getArrivalDelay")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getArrivalDelay")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException callNotPermittedException) {
                        log.error("Circuit breaker is open", callNotPermittedException);
                    }
                    if (throwable instanceof RequestNotPermitted requestNotPermittedException) {
                        log.error("Rate limit is exceeded", requestNotPermittedException);
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
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getDepartureDelay")))
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
