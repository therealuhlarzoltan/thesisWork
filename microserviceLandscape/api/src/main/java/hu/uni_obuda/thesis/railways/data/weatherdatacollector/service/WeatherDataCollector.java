package hu.uni_obuda.thesis.railways.data.weatherdatacollector.service;


import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

public interface WeatherDataCollector {
    @GetMapping("/collect-weather-info")
    Mono<WeatherInfo> getWeatherInfo(@RequestParam String startStation, @RequestParam String endStation, @RequestParam ZonedDateTime departureTime, @RequestParam ZonedDateTime arrivalTime);
}
