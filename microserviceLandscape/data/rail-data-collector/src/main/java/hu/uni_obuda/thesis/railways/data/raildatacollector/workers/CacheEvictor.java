package hu.uni_obuda.thesis.railways.data.raildatacollector.workers;

import hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.TimetableCache;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CacheEvictor {

    private static final Logger LOG = LoggerFactory.getLogger(CacheEvictor.class);

    private final TimetableCache timetableCache;

    @ScheduledJob("timetableCacheEviction")
    public void evictCache() {
        LOG.info("Evicting timetable cache...");
        timetableCache.evictAll()
                .doOnSuccess(_ -> LOG.info("Timetable cache eviction completed."))
                .doOnError(e -> LOG.error("Failed to evict timetable cache", e))
                .subscribe();
    }
}