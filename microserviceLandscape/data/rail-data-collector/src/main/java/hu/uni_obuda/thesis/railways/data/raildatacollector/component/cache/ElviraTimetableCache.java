package hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface ElviraTimetableCache extends TimetableCache {
    Mono<Void> cache(String from, String to, LocalDate date, ElviraShortTimetableResponse timetable);
    Mono<ElviraShortTimetableResponse> get(String from, String to, LocalDate date);
}
