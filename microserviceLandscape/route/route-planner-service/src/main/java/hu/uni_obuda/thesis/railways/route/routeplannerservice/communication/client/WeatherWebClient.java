package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WeatherWebClient {
    Mono<WeatherInfo> makeWeatherRequest(String address, Double latitude, Double longitude, LocalDateTime date);
}
