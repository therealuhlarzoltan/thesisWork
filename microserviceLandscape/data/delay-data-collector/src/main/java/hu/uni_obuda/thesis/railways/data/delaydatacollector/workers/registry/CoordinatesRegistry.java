package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import reactor.core.publisher.Mono;


public interface CoordinatesRegistry {
    Mono<GeocodingResponse> waitForCoordinatesWithCorrelationId(String correlationId);
    Mono<GeocodingResponse> waitForCoordinates(String stationName);
    void onCoordinates(GeocodingResponse coordinates);
    void onCoordinatesWithCorrelationId(String correlationId, GeocodingResponse coordinates);
}
