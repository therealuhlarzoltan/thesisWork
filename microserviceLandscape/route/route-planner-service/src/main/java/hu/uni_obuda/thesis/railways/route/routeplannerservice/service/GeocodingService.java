package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import reactor.core.publisher.Mono;

public interface GeocodingService {
    Mono<GeocodingResponse> getCoordinates(String stationName);
}
