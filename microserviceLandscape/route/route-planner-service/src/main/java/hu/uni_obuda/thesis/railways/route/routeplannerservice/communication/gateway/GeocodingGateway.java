package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import reactor.core.publisher.Mono;

public interface GeocodingGateway {
    Mono<GeocodingResponse> getCoordinates(String stationName);
}
