package hu.uni_obuda.thesis.railways.data.raildatacollector.worker;

import hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.TimetableCache;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class CacheEvictor {
    private final TimetableCache timetableCache;

    @ScheduledJob("timetableCacheEviction")
    public void evictCache() {
        log.info("Evicting timetable cache...");
        timetableCache.evictAll()
                .doOnSuccess(_ -> log.info("Timetable cache eviction completed."))
                .doOnError(e -> log.error("Failed to evict timetable cache", e))
                .subscribe();
    }
}