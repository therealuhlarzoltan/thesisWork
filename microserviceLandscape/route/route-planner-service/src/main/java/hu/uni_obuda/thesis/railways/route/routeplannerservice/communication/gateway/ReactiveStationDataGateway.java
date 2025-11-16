package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.StationWebClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Primary
@Component
public class ReactiveStationDataGateway implements StationDataGateway {

    private final StationWebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public ReactiveStationDataGateway(@Qualifier("reactiveStationWebClient") StationWebClient webClient,
                                      CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry,
                                      RateLimiterRegistry rateLimiterRegistry) {
        this.webClient = webClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Mono<TrainRouteResponse> getTrainRoute(String trainNumber) {
        return webClient.makeTrainRouteRequest(trainNumber)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getTrainRoute")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getTrainRoute")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getTrainRoute")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException callNotPermittedException) {
                        log.error("Circuit breaker is open", callNotPermittedException);
                    }
                    if (throwable instanceof RequestNotPermitted requestNotPermittedException) {
                        log.error("Rate limit is exceeded", requestNotPermittedException);
                    }
                    log.warn("Returning empty train route response because of exception", throwable);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<TrainStationResponse> getTrainStation(String stationCode) {
        return webClient.makeStationRequest(stationCode)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getTrainStation")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getTrainStation")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getTrainStation")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException callNotPermittedException) {
                        log.error("Circuit breaker is open", callNotPermittedException);
                    }
                    if (throwable instanceof RequestNotPermitted requestNotPermittedException) {
                        log.error("Rate limit is exceeded", requestNotPermittedException);
                    }
                    log.warn("Returning default train station response because of exception", throwable);
                    return Mono.just(TrainStationResponse.builder().stationCode(stationCode).build());
                });
    }
}
