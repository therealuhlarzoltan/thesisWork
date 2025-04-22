package hu.uni_obuda.thesis.railways.data.geocodingservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.response.CoordinatesResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import reactor.core.publisher.Mono;

public interface MapsGateway {
    @CircuitBreaker(name = "geocodingApi", fallbackMethod = "handleGetCoordinatesFallback")
    @Retry(name = "geocodingApi")
    Mono<CoordinatesResponse> getCoordinates(String address);
}
