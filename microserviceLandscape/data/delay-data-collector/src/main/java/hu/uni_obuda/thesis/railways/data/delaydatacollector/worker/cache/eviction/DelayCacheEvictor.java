package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.DelayInfoCache;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DelayCacheEvictor {

    private final DelayInfoCache delayInfoCache;

    @ScheduledJob("delayCacheEviction")
    public void evict() {
        log.info("Evicting delay info cache...");
        delayInfoCache.evictAll()
            .doOnSuccess(_ -> log.info("Delay info cache eviction completed."))
            .doOnError(e -> log.error("Failed to evict delay info cache", e))
            .subscribe();
    }
}
