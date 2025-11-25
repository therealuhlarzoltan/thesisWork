package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.impl;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TrainStatusCacheImpl implements TrainStatusCache {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${caching.train-status.cache-duration:12}")
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
        String key = toKey(trainNumber, date);
        return redisTemplate.opsForValue()
                .set(key, "complete", Duration.ofHours(cacheDuration))
                .then(redisTemplate.opsForSet().add(KEY_SET_PREFIX, key))
                .then();
    }

    @Override
    public Mono<Void> markIncomplete(String trainNumber, LocalDate date) {
        String key = toKey(trainNumber, date);
        return redisTemplate.opsForValue()
                .set(key, "incomplete", Duration.ofHours(cacheDuration))
                .then(redisTemplate.opsForSet().add(KEY_SET_PREFIX, key))
                .then();
    }

    @Override
    public Mono<Void> evict(String trainNumber, LocalDate date) {
        String key = toKey(trainNumber, date);
        return redisTemplate.opsForValue().delete(key)
                .then(redisTemplate.opsForSet().remove(KEY_SET_PREFIX, key))
                .then();
    }

    @Override
    public Mono<Void> evictAll() {
        return redisTemplate.opsForSet()
                .members(KEY_SET_PREFIX)
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) {
                        return Mono.empty();
                    } else {
                        return redisTemplate.delete(Flux.fromIterable(keys))
                                .then(redisTemplate.delete(KEY_SET_PREFIX))
                                .then();
                    }
                });
    }
}
