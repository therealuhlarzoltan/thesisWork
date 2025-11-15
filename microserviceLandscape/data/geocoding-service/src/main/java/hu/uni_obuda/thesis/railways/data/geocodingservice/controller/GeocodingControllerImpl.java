package hu.uni_obuda.thesis.railways.data.geocodingservice.controller;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.geocodingservice.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("coordinates")
public class GeocodingControllerImpl implements GeocodingController {

    private final GeocodingService service;

    @Override
    public Mono<GeocodingResponse> getCoordinates(String address) {
        return service.getCoordinates(address);
    }
}
