package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import reactor.core.publisher.Mono;

public interface DelayInfoCache {

    public static final String CACHE_PREFIX = "delayInfo";

    Mono<Boolean> isDuplicate(DelayInfo delay);
    Mono<Void> cacheDelay(DelayInfo delay);

    default String toKey(DelayInfo delay) {
        return CACHE_PREFIX + ":" + delay.getTrainNumber() + ":" + delay.getStationCode() + ":" +
                delay.getDate().toEpochDay();
    }
}
