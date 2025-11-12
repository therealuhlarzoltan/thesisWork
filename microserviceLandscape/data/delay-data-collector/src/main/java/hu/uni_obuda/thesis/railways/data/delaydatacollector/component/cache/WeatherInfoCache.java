package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache;


import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface WeatherInfoCache {

    String CACHE_PREFIX = "weatherInfo";
    String KEY_SET_PREFIX = CACHE_PREFIX + ":" + "keys";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    Mono<Boolean> isCached(String stationName, LocalDateTime dateTime);
    Mono<Void> cacheWeatherInfo(WeatherInfo weatherInfo);
    Mono<WeatherInfo> retrieveWeatherInfo(String stationName, LocalDateTime dateTime);
    Mono<Void> evict(String stationName, LocalDateTime dateTime);
    Mono<Void> evictAll();

    default String toKey(String stationName, LocalDateTime dateTime) {
        return CACHE_PREFIX + ":" + stationName + ":" + dateTime.format(formatter);
    }

    default String toKey(WeatherInfo weatherInfo) {
        return toKey(weatherInfo.getAddress(), weatherInfo.getTime());
    }
}
