package hu.uni_obuda.thesis.railways.data.geocodingservice.communication.client;

import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.response.CoordinatesResponse;
import reactor.core.publisher.Mono;

public interface MapsWebClient {
    Mono<CoordinatesResponse> getCoordinates(String address);
}
