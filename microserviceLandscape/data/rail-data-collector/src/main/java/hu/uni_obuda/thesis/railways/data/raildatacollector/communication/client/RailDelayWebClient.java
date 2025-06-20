package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.TimetableResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RailDelayWebClient {
    Mono<ShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date);
    Mono<ShortTrainDetailsResponse> getShortTrainDetails(String trainUri);
    Mono<TimetableResponse> getTimetable(String from, String to, LocalDate date);
}
