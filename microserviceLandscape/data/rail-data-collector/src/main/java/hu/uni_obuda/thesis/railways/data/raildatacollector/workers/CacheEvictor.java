package hu.uni_obuda.thesis.railways.data.raildatacollector.workers;

import hu.uni_obuda.thesis.railways.data.raildatacollector.components.TimetableCache;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CacheEvictor {

    private static final Logger LOG = LoggerFactory.getLogger(CacheEvictor.class);

    private final TimetableCache timetableCache;

    // Runs every day at 2 AM server time
    @Scheduled(cron = "0 0 2 * * *")
    public void evictCacheAt2AM() {
        LOG.info("Evicting timetable cache at 2 AM...");
        timetableCache.evictAll()
                .doOnSuccess(_ -> LOG.info("Timetable cache eviction completed."))
                .doOnError(e -> LOG.error("Failed to evict timetable cache", e))
                .subscribe();
    }
}
