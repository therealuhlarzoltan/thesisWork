package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class WeatherCacheEvictor {

    private final WeatherInfoCache weatherInfoCache;

    @ScheduledJob("weatherCacheEviction")
    public void evict() {
        log.info("Evicting weather info cache...");
        weatherInfoCache.evictAll()
                .doOnSuccess(_ -> log.info("Weather info cache eviction completed."))
                .doOnError(e -> log.error("Failed to evict weather info cache", e))
                .subscribe();
    }
}
