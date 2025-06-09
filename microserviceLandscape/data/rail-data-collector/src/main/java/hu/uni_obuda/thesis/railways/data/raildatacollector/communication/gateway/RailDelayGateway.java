package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.TimetableResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RailDelayGateway {
    Mono<ShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date);
    Mono<ShortTrainDetailsResponse> getShortTrainDetails(String trainUri);

    @CircuitBreaker(name = "getFullTimetableApi", fallbackMethod = "handleTimetableFallback")
    @Retry(name = "getFullTimetableApi")
    Mono<TimetableResponse> getTimetable(String from, String to, LocalDate date);
}
