package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.cache;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.TrainStatusCache;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TrainStatusCacheEvictor {

    private static final Logger LOG = LoggerFactory.getLogger(TrainStatusCacheEvictor.class);

    private final TrainStatusCache trainStatusCache;

    @Scheduled(cron = "0 0 3 * * *")
    public void evict() {
        LOG.info("Evicting train status cache at 3 AM...");
        trainStatusCache.evictAll()
                .doOnSuccess(_ -> LOG.info("TrainStatus cache eviction completed."))
                .doOnError(e -> LOG.error("Failed to evict train status cache", e))
                .subscribe();
    }
}
