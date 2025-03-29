package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.CoordinatesResponse;
import reactor.core.publisher.Mono;

public interface MapsGateway {
    Mono<CoordinatesResponse> getCoordinates(String address);
}
