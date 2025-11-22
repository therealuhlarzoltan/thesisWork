package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.GeocodingWebClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveGeocodingGatewayTest {

    @Mock
    private GeocodingWebClient webClient;

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

    private ReactiveGeocodingGateway testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveGeocodingGateway(
                webClient,
                circuitBreakerRegistry,
                retryRegistry,
                rateLimiterRegistry
        );
    }

    @Test
    void getCoordinates_success_returnsClientMono() {
        String stationName = "Budapest-Keleti";

        GeocodingResponse response = GeocodingResponse.builder()
                .latitude(47.5000)
                .longitude(19.0833)
                .address("Budapest, Keleti pályaudvar")
                .build();

        when(webClient.makeGeocodingRequest(stationName))
                .thenReturn(Mono.just(response));

        Mono<GeocodingResponse> result = testedObject.getCoordinates(stationName);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        verify(webClient).makeGeocodingRequest(stationName);
    }

    @Test
    void getCoordinates_requestNotPermitted_returnsEmptyMono() {
        String stationName = "Budapest-Keleti";

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("getCoordinates");
        RequestNotPermitted exception =
                RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        when(webClient.makeGeocodingRequest(stationName))
                .thenReturn(Mono.error(exception));

        Mono<GeocodingResponse> result = testedObject.getCoordinates(stationName);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeGeocodingRequest(stationName);
    }

    @Test
    void getCoordinates_callNotPermitted_returnsEmptyMono() {
        String stationName = "Budapest-Keleti";

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("getCoordinates");
        CallNotPermittedException exception =
                CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(webClient.makeGeocodingRequest(stationName))
                .thenReturn(Mono.error(exception));

        Mono<GeocodingResponse> result = testedObject.getCoordinates(stationName);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeGeocodingRequest(stationName);
    }

    @Test
    void getCoordinates_otherException_returnsEmptyMono() {
        String stationName = "Budapest-Keleti";

        RuntimeException exception = new RuntimeException("boom");

        when(webClient.makeGeocodingRequest(stationName))
                .thenReturn(Mono.error(exception));

        Mono<GeocodingResponse> result = testedObject.getCoordinates(stationName);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeGeocodingRequest(stationName);
    }
}
