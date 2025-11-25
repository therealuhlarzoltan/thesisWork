package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface EmmaRailDataGateway {
    Mono<EmmaShortTimetableResponse> getShortTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date);
    Mono<EmmaShortTrainDetailsResponse> getShortTrainDetails(String trainId, LocalDate serviceDate);
    Mono<EmmaTimetableResponse> getTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date);
}
