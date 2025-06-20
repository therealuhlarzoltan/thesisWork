package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client.WeatherDataWebClient;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherDataGatewayImpl implements WeatherDataGateway {

    private final WeatherDataWebClient weatherDataClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @Override
    public Mono<WeatherResponse> getWeatherByCoordinates(double latitude, double longitude, LocalDate date) {
        return weatherDataClient.getWeatherByCoordinates(latitude, longitude, date)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getWeatherDataApi")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getWeatherDataApi")))
                .onErrorResume(this::handleWeatherDataFallback);

    }

    public Mono<WeatherResponse> handleWeatherDataFallback(Throwable throwable) {
        if (throwable instanceof CallNotPermittedException callNotPermittedException) {
            log.error("Circuit breaker is open", callNotPermittedException);
        }
        return Mono.just(WeatherResponse.builder().build());
    }
}
