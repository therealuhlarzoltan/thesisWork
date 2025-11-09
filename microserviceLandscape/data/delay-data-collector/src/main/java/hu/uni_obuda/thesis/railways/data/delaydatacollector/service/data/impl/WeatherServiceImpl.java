package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.impl;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.WeatherService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.senders.MessageSender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry.WeatherInfoRegistry;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class WeatherServiceImpl implements WeatherService {

    private final WeatherInfoCache cache;
    private final MessageSender messageSender;
    private final WeatherInfoRegistry registry;

    @Override
    public Mono<WeatherInfo> getWeatherInfo(String stationName, Double latitude, Double longitude, LocalDateTime dateTime) {
        return cache.isCached(stationName, dateTime).flatMap(cached -> {
            if (Boolean.TRUE.equals(cached)) {
                return cache.retrieveWeatherInfo(stationName, dateTime);
            } else {
                Mono<WeatherInfo> responseMono = registry.waitForWeather(stationName, dateTime);
                messageSender.sendMessage("weatherDataRequests-out-0", constructWeatherRequestEvent(stationName, latitude, longitude, dateTime));
                return responseMono;
            }
        });
    }

    private CrudEvent<String, WeatherInfoRequest> constructWeatherRequestEvent(String stationName, Double latitude, Double longitude, LocalDateTime dateTime) {
        String key = stationName + ":" + dateTime.toString();
        WeatherInfoRequest request = new WeatherInfoRequest(stationName, latitude, longitude, dateTime);
        return new CrudEvent<>(CrudEvent.Type.GET, key, request);
    }
}
