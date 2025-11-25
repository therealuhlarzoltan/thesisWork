package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.impl;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.DelayInfoCache;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class DelayInfoCacheImpl implements DelayInfoCache {

    private final ReactiveRedisTemplate<String, String> delaysRedisTemplate;
    private final ReactiveRedisTemplate<String, String> keysRedisTemplate;

    @Value("${caching.delay.cache-duration:6}")
    private Integer cacheDuration;

    @Override
    public Mono<Boolean> isDuplicate(DelayInfo delay) {
        String key = toKey(delay);
        return delaysRedisTemplate.opsForValue()
                .get(key)
                .hasElement();
    }

    @Override
    public Mono<Void> cacheDelay(DelayInfo delay) {
        String key = toKey(delay);
        return delaysRedisTemplate.opsForValue()
                .set(key, "1", Duration.ofHours(cacheDuration))
                .then(keysRedisTemplate.opsForSet().add(KEY_SET_PREFIX, key))
                .then();
    }

    @Override
    public Mono<Void> evict(DelayInfo delay) {
        String key = toKey(delay);
        return delaysRedisTemplate.delete(key)
                .then(keysRedisTemplate.opsForSet().remove(KEY_SET_PREFIX, key))
                .then();
    }

    @Override
    public Mono<Void> evictAll() {
        return keysRedisTemplate.opsForSet()
                .members(KEY_SET_PREFIX)
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) {
                        return Mono.empty();
                    } else {
                        return delaysRedisTemplate.delete(Flux.fromIterable(keys))
                                .then(keysRedisTemplate.delete(KEY_SET_PREFIX))
                                .then();
                    }
                });
    }
}
