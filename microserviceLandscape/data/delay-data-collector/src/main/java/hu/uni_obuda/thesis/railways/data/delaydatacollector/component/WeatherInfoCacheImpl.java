package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class WeatherInfoCacheImpl implements WeatherInfoCache {

    private final ReactiveRedisTemplate<String, WeatherInfo> redisTemplate;

    @Value("${caching.weather.cache-duration:12}")
    private Integer cacheDuration;

    @Override
    public Mono<Boolean> isCached(String stationName, LocalDateTime dateTime) {
        return redisTemplate.hasKey(toKey(stationName, dateTime));
    }

    @Override
    public Mono<Void> cacheWeatherInfo(WeatherInfo weatherInfo) {
        String key = toKey(weatherInfo);
        return redisTemplate.opsForValue()
                .set(key, weatherInfo, Duration.ofHours(cacheDuration))
                .then();
    }

    @Override
    public Mono<WeatherInfo> retrieveWeatherInfo(String stationName, LocalDateTime dateTime) {
        return redisTemplate.opsForValue().get(toKey(stationName, dateTime));
    }
}
