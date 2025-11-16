package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.ElviraRailWebClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Profile("data-source-elvira")
@Primary
@Component
@Slf4j
public class ReactiveElviraRailDataGateway implements ElviraRailDataGateway {

    private final ElviraRailWebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public ReactiveElviraRailDataGateway(@Qualifier("reactiveElviraRailWebClient") ElviraRailWebClient webClient,
                                         CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry,
                                         RateLimiterRegistry rateLimiterRegistry) {
        this.webClient = webClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Flux<TrainRouteResponse> getTimetable(String from, String to, LocalDate date) {
        return webClient.makeRouteRequest(from, to, date)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getTimetable")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getTimetable")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getTimetable")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException callNotPermittedException) {
                        log.error("Circuit breaker is open", callNotPermittedException);
                    }
                    if (throwable instanceof RequestNotPermitted requestNotPermittedException) {
                        log.error("Rate limit is exceeded", requestNotPermittedException);
                    }
                    log.warn("Returning empty timetable response because of exception", throwable);
                    return Flux.empty();
                });
    }
}
