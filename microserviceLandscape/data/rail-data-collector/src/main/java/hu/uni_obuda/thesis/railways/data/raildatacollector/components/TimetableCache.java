package hu.uni_obuda.thesis.railways.data.raildatacollector.components;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.GraphQlShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface TimetableCache {

    String CACHE_PREFIX = "timetableResponse";
    String KEY_SET_PREFIX = CACHE_PREFIX + ":" + "keys";

    Mono<Boolean> isCached(String from, String to, LocalDate date);
    Mono<Void> cache(String from, String to, LocalDate date, ShortTimetableResponse timetable);

    Mono<Void> cache(String from, String to, LocalDate date, GraphQlShortTimetableResponse timetable);

    Mono<ShortTimetableResponse> get(String from, String to, LocalDate date);
    Mono<GraphQlShortTimetableResponse> getGraphQl(String from, String to, LocalDate date);
    Mono<Void> evictAll();

    default String toKey(String from, String to, LocalDate date) {
        return CACHE_PREFIX + ":" + from + ":" + to + ":" + date.toString();
    }
}
