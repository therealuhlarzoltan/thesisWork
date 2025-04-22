package hu.uni_obuda.thesis.railways.data.geocodingservice.controller;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

public interface GeocodingController {
    @GetMapping
    Mono<GeocodingResponse> getCoordinates(@RequestParam String address);
}
