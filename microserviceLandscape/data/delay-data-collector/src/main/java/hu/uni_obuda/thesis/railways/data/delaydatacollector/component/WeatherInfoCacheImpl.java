package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class WeatherInfoCacheImpl implements WeatherInfoCache {

    private final ReactiveRedisTemplate<String, WeatherInfo> redisTemplate;

    @Override
    public Mono<Boolean> isCached(String stationName, LocalDateTime dateTime) {
        return null;
    }

    @Override
    public Mono<Void> cacheWeatherInfo(WeatherInfo weatherInfo) {
        return null;
    }

    @Override
    public Mono<WeatherInfo> retrieveWeatherInfo(String stationName, LocalDateTime dateTime) {
        return null;
    }
}
