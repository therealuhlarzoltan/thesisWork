package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WeatherService {
    Mono<WeatherInfo> getWeatherInfo(String stationName, LocalDateTime dateTime);
}
