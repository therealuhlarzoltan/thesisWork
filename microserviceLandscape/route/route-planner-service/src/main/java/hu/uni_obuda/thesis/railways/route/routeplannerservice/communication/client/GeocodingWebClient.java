package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import reactor.core.publisher.Mono;

public interface GeocodingWebClient {
    Mono<GeocodingResponse> makeGeocodingRequest(String stationName);
}
