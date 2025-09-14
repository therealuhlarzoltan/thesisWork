package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.TimetableResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RailDelayWebClient {
    default Mono<ShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<ShortTrainDetailsResponse> getShortTrainDetails(String trainUri) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<TimetableResponse> getTimetable(String from, String to, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<ShortTimetableResponse> getShortTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<ShortTrainDetailsResponse> getShortTrainDetails(String trainId, LocalDate serviceDate) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
    default Mono<TimetableResponse> getTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    };
}
