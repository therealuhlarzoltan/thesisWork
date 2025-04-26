package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class CoordinatesCacheImpl implements CoordinatesCache {

    private final ReactiveRedisTemplate<String, GeocodingResponse> coordinatesRedisTemplate;
    private final ReactiveRedisTemplate<String, String> keysRedisTemplate;

    @Override
    public Mono<Boolean> isCached(String stationName) {
        return coordinatesRedisTemplate.hasKey(toKey(stationName));
    }

    @Override
    public Mono<Void> cache(String stationName, GeocodingResponse coordinates) {
        String key = toKey(stationName);
        return coordinatesRedisTemplate
                .opsForValue()
                .set(key, coordinates)
                .then(keysRedisTemplate.opsForSet().add(KEY_SET_PREFIX, key))
                .then();
    }

    @Override
    public Mono<GeocodingResponse> get(String stationName) {
        return coordinatesRedisTemplate.opsForValue().get(toKey(stationName));
    }


    @Override
    public Mono<Void> evict(String stationName) {
        return coordinatesRedisTemplate.delete(stationName).then(keysRedisTemplate.delete(stationName).then());
    }

    @Override
    public Mono<Void> evictAll() {
        return keysRedisTemplate
                .opsForSet()
                .members(KEY_SET_PREFIX)
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) return Mono.empty();
                    return coordinatesRedisTemplate.delete(Flux.fromIterable(keys))
                            .then(keysRedisTemplate.delete(KEY_SET_PREFIX))
                            .then();
                });
    }
}
