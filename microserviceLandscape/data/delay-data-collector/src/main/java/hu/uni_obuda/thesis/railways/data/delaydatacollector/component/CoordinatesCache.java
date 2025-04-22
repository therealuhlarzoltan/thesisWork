package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface CoordinatesCache {

    String CACHE_PREFIX = "coordinates";
    String KEY_SET_PREFIX = CACHE_PREFIX + ":" + "keys";


    Mono<Boolean> isCached(String stationName);
    Mono<Void> cache(String stationName, GeocodingResponse coordinates);
    Mono<GeocodingResponse> get(String stationName);
    Mono<Void> evict(String stationName);
    Mono<Void> evictAll();

    default String toKey(String stationName) {
        return CACHE_PREFIX + ":" + stationName;
    }
}

