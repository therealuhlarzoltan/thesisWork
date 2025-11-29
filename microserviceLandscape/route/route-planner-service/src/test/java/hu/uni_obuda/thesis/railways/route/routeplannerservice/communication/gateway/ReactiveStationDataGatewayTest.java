package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.StationWebClient;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveStationDataGatewayTest {

    @Mock
    private StationWebClient webClient;

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

    private ReactiveStationDataGateway testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveStationDataGateway(
                webClient,
                circuitBreakerRegistry,
                retryRegistry,
                rateLimiterRegistry
        );
    }

    @Test
    void getTrainRoute_success_returnsClientMono() {
        String trainNumber = "IC123";

        TrainRouteResponse route = new TrainRouteResponse(
                "IC123",
                "80",
                "Budapest-Keleti",
                "Miskolc-Tiszai"
        );

        when(webClient.makeTrainRouteRequest(trainNumber))
                .thenReturn(Mono.just(route));

        Mono<TrainRouteResponse> result = testedObject.getTrainRoute(trainNumber);

        StepVerifier.create(result)
                .expectNext(route)
                .verifyComplete();

        verify(webClient).makeTrainRouteRequest(trainNumber);
    }

    @Test
    void getTrainRoute_requestNotPermitted_returnsEmptyMono() {
        String trainNumber = "IC123";

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("getTrainRoute");
        RequestNotPermitted exception =
                RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        when(webClient.makeTrainRouteRequest(trainNumber))
                .thenReturn(Mono.error(exception));

        Mono<TrainRouteResponse> result = testedObject.getTrainRoute(trainNumber);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeTrainRouteRequest(trainNumber);
    }

    @Test
    void getTrainRoute_callNotPermitted_returnsEmptyMono() {
        String trainNumber = "IC123";

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("getTrainRoute");
        CallNotPermittedException exception =
                CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(webClient.makeTrainRouteRequest(trainNumber))
                .thenReturn(Mono.error(exception));

        Mono<TrainRouteResponse> result = testedObject.getTrainRoute(trainNumber);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeTrainRouteRequest(trainNumber);
    }

    @Test
    void getTrainRoute_otherException_returnsEmptyMono() {
        String trainNumber = "IC123";

        RuntimeException exception = new RuntimeException("boom");

        when(webClient.makeTrainRouteRequest(trainNumber))
                .thenReturn(Mono.error(exception));

        Mono<TrainRouteResponse> result = testedObject.getTrainRoute(trainNumber);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeTrainRouteRequest(trainNumber);
    }

    @Test
    void getTrainStation_success_returnsClientMono() {
        String stationCode = "BPK";

        TrainStationResponse response = TrainStationResponse.builder()
                .stationCode("BPK")
                .latitude(47.5000)
                .longitude(19.0833)
                .build();

        when(webClient.makeStationRequest(stationCode))
                .thenReturn(Mono.just(response));

        Mono<TrainStationResponse> result = testedObject.getTrainStation(stationCode);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        verify(webClient).makeStationRequest(stationCode);
    }

    @Test
    void getTrainStation_requestNotPermitted_returnsDefaultResponse() {
        String stationCode = "BPK";

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("getTrainStation");
        RequestNotPermitted exception =
                RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        when(webClient.makeStationRequest(stationCode))
                .thenReturn(Mono.error(exception));

        Mono<TrainStationResponse> result = testedObject.getTrainStation(stationCode);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("BPK", resp.getStationCode());
                    assertNull(resp.getLatitude());
                    assertNull(resp.getLongitude());
                })
                .verifyComplete();

        verify(webClient).makeStationRequest(stationCode);
    }

    @Test
    void getTrainStation_callNotPermitted_returnsDefaultResponse() {
        String stationCode = "BPK";

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("getTrainStation");
        CallNotPermittedException exception =
                CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(webClient.makeStationRequest(stationCode))
                .thenReturn(Mono.error(exception));

        Mono<TrainStationResponse> result = testedObject.getTrainStation(stationCode);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("BPK", resp.getStationCode());
                    assertNull(resp.getLatitude());
                    assertNull(resp.getLongitude());
                })
                .verifyComplete();

        verify(webClient).makeStationRequest(stationCode);
    }

    @Test
    void getTrainStation_otherException_returnsDefaultResponse() {
        String stationCode = "BPK";

        RuntimeException exception = new RuntimeException("boom");

        when(webClient.makeStationRequest(stationCode))
                .thenReturn(Mono.error(exception));

        Mono<TrainStationResponse> result = testedObject.getTrainStation(stationCode);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("BPK", resp.getStationCode());
                    assertNull(resp.getLatitude());
                    assertNull(resp.getLongitude());
                })
                .verifyComplete();

        verify(webClient).makeStationRequest(stationCode);
    }
}
