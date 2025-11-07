package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import reactor.core.publisher.Mono;

public interface DelayInfoCache {

    String CACHE_PREFIX = "delayInfo";
    String KEY_SET_PREFIX = CACHE_PREFIX + ":" + "keys";

    Mono<Boolean> isDuplicate(DelayInfo delay);
    Mono<Void> cacheDelay(DelayInfo delay);
    Mono<Void> evict(DelayInfo delay);
    Mono<Void> evictAll();

    default String toKey(DelayInfo delay) {
        return CACHE_PREFIX + ":" + delay.getTrainNumber() + ":" + delay.getStationCode() + ":" +
                delay.getDate().toEpochDay();
    }
}
