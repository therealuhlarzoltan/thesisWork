package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RailDelayWebClient {
    default Mono<ElviraShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<ElviraShortTrainDetailsResponse> getShortTrainDetails(String trainUri) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<ElviraTimetableResponse> getTimetable(String from, String to, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<EmmaShortTimetableResponse> getShortTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<EmmaShortTrainDetailsResponse> getShortTrainDetails(String trainId, LocalDate serviceDate) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<EmmaTimetableResponse> getTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
}
