package hu.uni_obuda.thesis.railways.data.weatherdatacollector.service;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WeatherDataService {
    Mono<WeatherInfo> getWeatherInfoByAddress(String address, LocalDateTime localDateTime);
}
