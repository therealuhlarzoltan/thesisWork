package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.MessageSender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.WeatherInfoRegistry;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class WeatherServiceImpl implements WeatherService {

    private final MessageSender messageSender;
    private final WeatherInfoRegistry registry;

    @Cacheable(value = "weather", key = "#stationName + ':' + #dateTime.truncatedTo(T(java.time.temporal.ChronoUnit).HOURS).toString()")
    @Override
    public Mono<WeatherInfo> getWeatherInfo(String stationName, LocalDateTime dateTime) {
        Mono<WeatherInfo> responseMono = registry.waitForWeather(stationName, dateTime);
        messageSender.sendMessage("weatherDataRequests-out-0", constructWeatherRequestEvent(stationName, dateTime));
        return responseMono;
    }

    private CrudEvent<String, WeatherInfoRequest> constructWeatherRequestEvent(String stationName, LocalDateTime dateTime) {
        String key = stationName + ":" + dateTime.toString();
        WeatherInfoRequest request = new WeatherInfoRequest(stationName, dateTime);
        return new CrudEvent<>(CrudEvent.Type.GET, key, request);
    }
}
