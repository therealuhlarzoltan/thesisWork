package hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTimetableResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface EmmaTimetableCache extends TimetableCache {
    Mono<Void> cache(String from, String to, LocalDate date, EmmaShortTimetableResponse timetable);
    Mono<EmmaShortTimetableResponse> get(String from, String to, LocalDate date);
}
