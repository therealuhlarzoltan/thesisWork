package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RailDelayGateway {
    Mono<ShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date);
    Mono<ShortTrainDetailsResponse> getShortTrainDetails(String trainUri);
}
