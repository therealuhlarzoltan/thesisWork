package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache;

import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface TrainStatusCache {

    String CACHE_PREFIX = "trainStatus";
    String KEY_SET_PREFIX = CACHE_PREFIX + ":" + "keys";


    Mono<Boolean> isComplete(String trainNumber, LocalDate date);
    Mono<Void> markComplete(String trainNumber, LocalDate date);
    Mono<Void> markIncomplete(String trainNumber, LocalDate date);
    Mono<Void> evict(String trainNumber, LocalDate date);
    Mono<Void> evictAll();

    default String toKey(String trainNumber, LocalDate date) {
        return CACHE_PREFIX + ":" + trainNumber + ":" + date;
    }
}
