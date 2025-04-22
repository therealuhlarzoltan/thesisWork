package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface CoordinatesRegistry {
    Mono<GeocodingResponse> waitForCoordinatesWithCorrelationId(String correlationId, String stationName);
    Mono<GeocodingResponse> waitForCoordinates(String stationName);
    void onCoordinates(String stationName, GeocodingResponse coordinates);
    void onCoordinates(String correlationId, String stationName, GeocodingResponse coordinates);
}
