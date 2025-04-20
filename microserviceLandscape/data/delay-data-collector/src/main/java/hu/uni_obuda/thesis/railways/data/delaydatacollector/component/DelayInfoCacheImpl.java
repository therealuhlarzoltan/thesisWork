package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class DelayInfoCacheImpl implements DelayInfoCache {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${caching.delay.cache-duration:6}")
    private Integer cacheDuration;

    @Override
    public Mono<Boolean> isDuplicate(DelayInfo delay) {
        String key = toKey(delay);
        return redisTemplate.opsForValue()
                .get(key)
                .hasElement();
    }

    @Override
    public Mono<Void> cacheDelay(DelayInfo delay) {
        String key = toKey(delay);
        return redisTemplate.opsForValue()
                .set(key, "1", Duration.ofHours(cacheDuration))
                .then();
    }
}
