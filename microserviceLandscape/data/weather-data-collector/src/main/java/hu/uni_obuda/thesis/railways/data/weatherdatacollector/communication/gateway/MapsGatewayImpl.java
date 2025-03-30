package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client.MapsWebClient;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.CoordinatesResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;

@Component
@RequiredArgsConstructor
public class MapsGatewayImpl implements MapsGateway {

    private final MapsWebClient mapsClient;

    @CircuitBreaker(name = "geocodingApi", fallbackMethod = "handleGetCoordinatesFallback")
    @Retry(name = "geocodingApi")
    @Override
    public Mono<CoordinatesResponse> getCoordinates(String address) {
        return mapsClient.getCoordinates(address);
    }

    public Mono<CoordinatesResponse> handleGetCoordinatesFallback(String trainUri, Throwable throwable) throws MalformedURLException {
        return Mono.error(resolveApiException(throwable));
    }

    private ApiException resolveApiException(Throwable throwable) throws MalformedURLException {
        if (throwable instanceof WebClientResponseException response) {
            return new ExternalApiException(response.getStatusCode(), response.getRequest().getURI().toURL());
        } else if (throwable instanceof WebClientRequestException request) {
            return new InternalApiException(request.getMessage(), request.getUri().toURL());
        } else {
            return new InternalApiException("A runtime exception occurred", null);
        }
    }
}