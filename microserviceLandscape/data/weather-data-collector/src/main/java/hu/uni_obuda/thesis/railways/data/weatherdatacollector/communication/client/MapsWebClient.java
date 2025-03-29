package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.CoordinatesResponse;
import reactor.core.publisher.Mono;

public interface MapsWebClient {
    Mono<CoordinatesResponse> getCoordinates(String address);
}
