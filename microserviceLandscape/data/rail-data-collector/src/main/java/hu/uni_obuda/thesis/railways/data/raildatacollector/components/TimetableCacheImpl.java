package hu.uni_obuda.thesis.railways.data.raildatacollector.components;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class TimetableCacheImpl implements TimetableCache {

    private final ReactiveRedisTemplate<String, ShortTimetableResponse> reactiveRedisTemplate;

    @Value("${caching.timetable.cache-duration:6}")
    private Integer cacheDuration;

    @Override
    public Mono<Boolean> isCached(String from, String to, LocalDate date) {
        return reactiveRedisTemplate.hasKey(toKey(from, to, date));
    }

    @Override
    public Mono<Void> cache(String from, String to, LocalDate date, ShortTimetableResponse timetable) {
        String key = toKey(from, to, date);
        return reactiveRedisTemplate
                .opsForValue()
                .set(key, timetable, Duration.ofHours(cacheDuration))
                .then();
    }

    @Override
    public Mono<ShortTimetableResponse> get(String from, String to, LocalDate date) {
        return reactiveRedisTemplate.opsForValue().get(toKey(from, to, date));
    }

    @Override
    public Mono<Void> evict() {
        return null;
    }
}
