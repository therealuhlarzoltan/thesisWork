package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WeatherDataGateway {
    Mono<WeatherInfo> getWeatherInfo(String address, Double latitude, Double longitude, LocalDateTime time);
}
