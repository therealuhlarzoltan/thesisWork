package hu.uni_obuda.thesis.railways.data.raildatacollector.components;

import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface TimetableCache {

    String CACHE_PREFIX = "timetableResponse";
    String KEY_SET_PREFIX = CACHE_PREFIX + ":" + "keys";

    Mono<Boolean> isCached(String from, String to, LocalDate date);
    Mono<Void> evictAll();

    default String toKey(String from, String to, LocalDate date) {
        return CACHE_PREFIX + ":" + from + ":" + to + ":" + date.toString();
    }
}
