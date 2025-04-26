package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.cache;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.WeatherInfoCache;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class WeatherCacheEvictor {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherCacheEvictor.class);

    private final WeatherInfoCache weatherInfoCache;

    @Scheduled(cron = "0 0 3 * * *")
    public void evict() {
        LOG.info("Evicting weather info cache at 3 AM...");
        weatherInfoCache.evictAll()
                .doOnSuccess(_ -> LOG.info("Weather info cache eviction completed."))
                .doOnError(e -> LOG.error("Failed to evict weather info cache", e))
                .subscribe();
    }
}
