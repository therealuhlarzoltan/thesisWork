package hu.uni_obuda.thesis.railways.data.geocodingservice.service;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import reactor.core.publisher.Mono;

public interface GeocodingService {
    Mono<GeocodingResponse> getCoordinates(String address);
}
