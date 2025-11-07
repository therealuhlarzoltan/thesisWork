package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.cache;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.DelayInfoCache;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DelayCacheEvictor {

    private static final Logger LOG = LoggerFactory.getLogger(DelayCacheEvictor.class);

    private final DelayInfoCache delayInfoCache;

    @ScheduledJob("delayCacheEviction")
    public void evict() {
        LOG.info("Evicting delay info cache at 3 AM...");
        delayInfoCache.evictAll()
            .doOnSuccess(_ -> LOG.info("Delay info cache eviction completed."))
            .doOnError(e -> LOG.error("Failed to evict delay info cache", e))
            .subscribe();
    }
}
