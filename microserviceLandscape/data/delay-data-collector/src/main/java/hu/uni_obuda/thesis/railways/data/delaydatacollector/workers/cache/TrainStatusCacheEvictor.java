package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.cache;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TrainStatusCacheEvictor {

    private static final Logger LOG = LoggerFactory.getLogger(TrainStatusCacheEvictor.class);

    private final TrainStatusCache trainStatusCache;

    @ScheduledJob("trainStatusCacheEvictor")
    public void evict() {
        LOG.info("Evicting train status cache at 3 AM...");
        trainStatusCache.evictAll()
                .doOnSuccess(_ -> LOG.info("TrainStatus cache eviction completed."))
                .doOnError(e -> LOG.error("Failed to evict train status cache", e))
                .subscribe();
    }
}
