package hu.uni_obuda.thesis.railways.data.delaydatacollector.component;

import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface TrainStatusCache {

    public static final String CACHE_PREFIX = "trainStatus";

    Mono<Boolean> isComplete(String trainNumber, LocalDate date);
    Mono<Void> markComplete(String trainNumber, LocalDate date);
    Mono<Void> markIncomplete(String trainNumber, LocalDate date);

    default String toKey(String trainNumber, LocalDate date) {
        return CACHE_PREFIX + ":" + trainNumber + ":" + date;
    }
}
