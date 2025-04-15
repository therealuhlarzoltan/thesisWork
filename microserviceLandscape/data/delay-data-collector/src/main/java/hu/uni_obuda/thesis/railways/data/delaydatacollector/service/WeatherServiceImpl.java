package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.WeatherInfoRegistry;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class WeatherServiceImpl implements WeatherService {

    private final WeatherInfoRegistry registry;

    @Cacheable(value = "weather", key = "#stationName + ':' + #dateTime.toString()")
    @Override
    public Mono<WeatherInfo> getWeatherInfo(String stationName, LocalDateTime dateTime) {
        return registry.waitForWeather(stationName, dateTime);
    }
}
