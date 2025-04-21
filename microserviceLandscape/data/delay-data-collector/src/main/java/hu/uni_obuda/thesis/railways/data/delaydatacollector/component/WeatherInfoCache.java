package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;


import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface WeatherInfoCache {

    String CACHE_PREFIX = "weatherInfo";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    Mono<Boolean> isCached(String stationName, LocalDateTime dateTime);
    Mono<Void> cacheWeatherInfo(WeatherInfo weatherInfo);
    Mono<WeatherInfo> retrieveWeatherInfo(String stationName, LocalDateTime dateTime);

    default String toKey(String stationName, LocalDateTime dateTime) {
        return CACHE_PREFIX + ":" + stationName + ":" + dateTime.format(formatter);
    }

    default String toKey(WeatherInfo weatherInfo) {
        return toKey(weatherInfo.getAddress(), weatherInfo.getTime());
    }
}
