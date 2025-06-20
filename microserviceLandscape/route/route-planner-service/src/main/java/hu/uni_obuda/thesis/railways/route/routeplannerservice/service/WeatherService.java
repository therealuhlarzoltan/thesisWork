package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WeatherService {
    Mono<WeatherInfo> getWeather(String stationName, Double latitude, Double longitude, LocalDateTime dateTime);
}
