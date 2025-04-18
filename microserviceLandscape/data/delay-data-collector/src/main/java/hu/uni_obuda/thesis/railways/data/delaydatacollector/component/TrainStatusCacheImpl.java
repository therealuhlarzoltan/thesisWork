package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class TrainStatusCacheImpl implements TrainStatusCache {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${caching.train-status.cache-duration}:12")
    private Integer cacheDuration;

    @Override
    public Mono<Boolean> isComplete(String trainNumber, LocalDate date) {
        String key = toKey(trainNumber, date);
        return redisTemplate.opsForValue()
                .get(key)
                .map("complete"::equalsIgnoreCase)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> markComplete(String trainNumber, LocalDate date) {
        return redisTemplate.opsForValue()
                .set(toKey(trainNumber, date), "complete", Duration.ofDays(cacheDuration))
                .then();
    }

    @Override
    public Mono<Void> markIncomplete(String trainNumber, LocalDate date) {
        return redisTemplate.opsForValue()
                .set(toKey(trainNumber, date), "incomplete", Duration.ofHours(cacheDuration))
                .then();
    }
}
