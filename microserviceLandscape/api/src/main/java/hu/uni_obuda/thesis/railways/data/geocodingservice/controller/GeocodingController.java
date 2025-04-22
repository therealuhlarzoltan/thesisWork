package hu.uni_obuda.thesis.railways.data.geocodingservice.controller;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.CoordinatesResponse;
import reactor.core.publisher.Mono;

public interface GeocodingController {
    Mono<CoordinatesResponse> getCoordinates(String address);
}
