package hu.uni_obuda.thesis.railways.data.geocodingservice.controller;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import reactor.core.publisher.Mono;

public interface GeocodingController {
    Mono<GeocodingResponse> getCoordinates(String address);
}
