package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class TrainStatusCacheEvictor {

    private final TrainStatusCache trainStatusCache;

    @ScheduledJob("trainStatusCacheEviction")
    public void evict() {
        log.info("Evicting train status cache...");
        trainStatusCache.evictAll()
                .doOnSuccess(_ -> log.info("TrainStatus cache eviction completed."))
                .doOnError(e -> log.error("Failed to evict train status cache", e))
                .subscribe();
    }
}
