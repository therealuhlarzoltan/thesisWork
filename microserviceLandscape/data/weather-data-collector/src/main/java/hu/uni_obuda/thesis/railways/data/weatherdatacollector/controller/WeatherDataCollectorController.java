package hu.uni_obuda.thesis.railways.data.weatherdatacollector.controller;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.service.WeatherDataCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class WeatherDataCollectorController implements WeatherDataCollector {
    @Override
    public Mono<WeatherInfo> getWeatherInfo() {
        return null;
    }
}
