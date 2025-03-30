package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client.WeatherDataWebClient;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class WeatherDataGatewayImpl implements WeatherDataGateway {

    private final WeatherDataWebClient weatherDataClient;

    @CircuitBreaker(name = "getWeatherDataApi", fallbackMethod = "handleWeatherDataFallback")
    @Retry(name = "getWeatherDataApi")
    @Override
    public Mono<WeatherResponse> getWeatherByCoordinates(double latitude, double longitude, LocalDate date) {
        return weatherDataClient.getWeatherByCoordinates(latitude, longitude, date);
    }

    public Mono<WeatherResponse> handleGetWeatherDataFallback(double latitude, double longitude, LocalDate date, Throwable throwable) {
        return Mono.just(WeatherResponse.builder().build());
    }
}
