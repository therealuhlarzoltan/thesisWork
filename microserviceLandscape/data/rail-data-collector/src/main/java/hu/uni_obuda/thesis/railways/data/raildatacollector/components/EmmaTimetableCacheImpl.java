package hu.uni_obuda.thesis.railways.data.raildatacollector.components;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTimetableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;

@Profile("data-source-emma")
@Component
@RequiredArgsConstructor
public class EmmaTimetableCacheImpl implements EmmaTimetableCache {

    private final ReactiveRedisTemplate<String, EmmaShortTimetableResponse> timetableRedisTemplate;
    private final ReactiveRedisTemplate<String, String> keysRedisTemplate;

    @Value("${caching.timetable.cache-duration:6}")
    private Integer cacheDuration;

    @Override
    public Mono<Boolean> isCached(String from, String to, LocalDate date) {
        return timetableRedisTemplate.hasKey(toKey(from, to, date));
    }

    @Override
    public Mono<Void> cache(String from, String to, LocalDate date, EmmaShortTimetableResponse timetable) {
        String key = toKey(from, to, date);
        return timetableRedisTemplate
                .opsForValue()
                .set(key, timetable, Duration.ofHours(cacheDuration))
                .then(keysRedisTemplate.opsForSet().add(KEY_SET_PREFIX, key))
                .then();
    }

    @Override
    public Mono<EmmaShortTimetableResponse> get(String from, String to, LocalDate date) {
        return timetableRedisTemplate.opsForValue().get(toKey(from, to, date));
    }

    @Override
    public Mono<Void> evictAll() {
        return keysRedisTemplate
                .opsForSet()
                .members(KEY_SET_PREFIX)
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) return Mono.empty();
                    return timetableRedisTemplate.delete(Flux.fromIterable(keys))
                            .then(keysRedisTemplate.delete(KEY_SET_PREFIX))
                            .then();
                });
    }
}
