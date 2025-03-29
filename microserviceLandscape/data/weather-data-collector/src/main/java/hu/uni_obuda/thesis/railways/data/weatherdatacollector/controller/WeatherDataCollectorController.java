package hu.uni_obuda.thesis.railways.data.weatherdatacollector.controller;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.service.WeatherDataCollector;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.service.WeatherDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class WeatherDataCollectorController implements WeatherDataCollector {

    private final WeatherDataService service;

    @Override
    public Mono<WeatherInfo> getWeatherInfo(String stationName, LocalDateTime dateTime) {
        return service.getWeatherInfoByAddress(stationName, dateTime);
    }
}
