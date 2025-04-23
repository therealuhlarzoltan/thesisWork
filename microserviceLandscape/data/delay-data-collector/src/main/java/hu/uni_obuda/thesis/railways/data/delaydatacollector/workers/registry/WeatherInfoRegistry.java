package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WeatherInfoRegistry {
    Mono<WeatherInfo> waitForWeather(String correlationId);
    Mono<WeatherInfo> waitForWeather(String stationName, LocalDateTime dateTime);
    void onWeatherInfo(WeatherInfo info);
    void onWeatherInfo(String correlationId, WeatherInfo info);
    void onError(String stationName, LocalDateTime dateTime, Throwable throwable);
    void onErrorWithCorrelationId(String correlationId, Throwable throwable);
}
