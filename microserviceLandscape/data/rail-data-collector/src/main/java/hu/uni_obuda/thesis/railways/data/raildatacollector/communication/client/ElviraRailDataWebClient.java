package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface ElviraRailDataWebClient {
    Mono<ElviraShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date);
    Mono<ElviraShortTrainDetailsResponse> getShortTrainDetails(String trainUri);
    Mono<ElviraTimetableResponse> getTimetable(String from, String to, LocalDate date);
}
