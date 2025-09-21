package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaTimetableResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface EmmaRailDataWebClient {
    Mono<EmmaShortTimetableResponse> getShortTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date);
    Mono<EmmaShortTrainDetailsResponse> getShortTrainDetails(String trainId, LocalDate serviceDate);
    Mono<EmmaTimetableResponse> getTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date);
}
