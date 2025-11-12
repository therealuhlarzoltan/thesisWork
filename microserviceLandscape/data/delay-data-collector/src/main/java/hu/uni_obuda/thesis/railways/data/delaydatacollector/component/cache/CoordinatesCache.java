package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CoordinatesCache {

    String CACHE_PREFIX = "coordinates";
    String KEY_SET_PREFIX = CACHE_PREFIX + ":" + "keys";

    Flux<GeocodingResponse> getAll();
    Mono<Boolean> isCached(String stationName);
    Mono<Void> cache(String stationName, GeocodingResponse coordinates);
    Mono<GeocodingResponse> get(String stationName);
    Mono<Void> evict(String stationName);
    Mono<Void> evictAll();

    default String toKey(String stationName) {
        return CACHE_PREFIX + ":" + stationName;
    }
}

