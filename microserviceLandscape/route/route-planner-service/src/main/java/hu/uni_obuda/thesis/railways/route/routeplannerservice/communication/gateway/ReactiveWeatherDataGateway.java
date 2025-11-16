package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.WeatherWebClient;
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
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Primary
@Component
public class ReactiveWeatherDataGateway implements WeatherDataGateway {

    private final WeatherWebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public ReactiveWeatherDataGateway(@Qualifier("reactiveWeatherWebClient") WeatherWebClient webClient,
                                      CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry,
                                      RateLimiterRegistry rateLimiterRegistry) {
        this.webClient = webClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Mono<WeatherInfo> getWeatherInfo(String address, Double latitude, Double longitude, LocalDateTime time) {
        return webClient.makeWeatherRequest(address, latitude, longitude, time)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getWeatherInfo")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getWeatherInfo")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getWeatherInfo")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof CallNotPermittedException callNotPermittedException) {
                        log.error("Circuit breaker is open", callNotPermittedException);
                    }
                    if (throwable instanceof RequestNotPermitted requestNotPermittedException) {
                        log.error("Rate limit is exceeded", requestNotPermittedException);
                    }
                    log.warn("Returning default weather info response because of exception", throwable);
                    return Mono.just(WeatherInfo.builder().address(address).latitude(latitude).longitude(longitude).time(time).build());
                });
    }
}
