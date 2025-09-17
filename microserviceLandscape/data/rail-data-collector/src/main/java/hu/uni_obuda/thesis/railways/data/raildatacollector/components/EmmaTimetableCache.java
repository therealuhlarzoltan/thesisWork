package hu.uni_obuda.thesis.railways.data.raildatacollector.components;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.GraphQlShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface EmmaTimetableCache extends TimetableCache {
    Mono<Void> cache(String from, String to, LocalDate date, GraphQlShortTimetableResponse timetable);
    Mono<GraphQlShortTimetableResponse> get(String from, String to, LocalDate date);
}
