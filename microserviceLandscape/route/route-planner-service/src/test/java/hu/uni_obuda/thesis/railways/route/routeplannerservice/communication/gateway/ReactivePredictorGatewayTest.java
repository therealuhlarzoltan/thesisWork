package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.PredictorWebClient;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactivePredictorGatewayTest {

    @Mock
    private PredictorWebClient webClient;

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

    private ReactivePredictorGateway testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactivePredictorGateway(
                webClient,
                circuitBreakerRegistry,
                retryRegistry,
                rateLimiterRegistry
        );
    }

    @Test
    void getArrivalDelay_success_returnsClientMono() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .build();

        DelayPredictionResponse response = DelayPredictionResponse.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .predictedDelay(12d)
                .build();

        when(webClient.makeArrivalPredictionRequest(request))
                .thenReturn(Mono.just(response));

        Mono<DelayPredictionResponse> result = testedObject.getArrivalDelay(request);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        verify(webClient).makeArrivalPredictionRequest(request);
    }

    @Test
    void getArrivalDelay_requestNotPermitted_returnsDefaultResponse() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .build();

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("getArrivalDelay");
        RequestNotPermitted exception =
                RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        when(webClient.makeArrivalPredictionRequest(request))
                .thenReturn(Mono.error(exception));

        Mono<DelayPredictionResponse> result = testedObject.getArrivalDelay(request);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("IC123", resp.getTrainNumber());
                    assertEquals("BPK", resp.getStationCode());
                    assertEquals(0d, resp.getPredictedDelay());
                })
                .verifyComplete();

        verify(webClient).makeArrivalPredictionRequest(request);
    }

    @Test
    void getArrivalDelay_callNotPermitted_returnsDefaultResponse() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .build();

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("getArrivalDelay");
        CallNotPermittedException exception =
                CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(webClient.makeArrivalPredictionRequest(request))
                .thenReturn(Mono.error(exception));

        Mono<DelayPredictionResponse> result = testedObject.getArrivalDelay(request);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("IC123", resp.getTrainNumber());
                    assertEquals("BPK", resp.getStationCode());
                    assertEquals(0d, resp.getPredictedDelay());
                })
                .verifyComplete();

        verify(webClient).makeArrivalPredictionRequest(request);
    }

    @Test
    void getArrivalDelay_otherException_returnsDefaultResponse() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .build();

        RuntimeException exception = new RuntimeException("boom");

        when(webClient.makeArrivalPredictionRequest(request))
                .thenReturn(Mono.error(exception));

        Mono<DelayPredictionResponse> result = testedObject.getArrivalDelay(request);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("IC123", resp.getTrainNumber());
                    assertEquals("BPK", resp.getStationCode());
                    assertEquals(0d, resp.getPredictedDelay());
                })
                .verifyComplete();

        verify(webClient).makeArrivalPredictionRequest(request);
    }

    @Test
    void getDepartureDelay_success_returnsClientMono() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC456")
                .stationCode("DEB")
                .build();

        DelayPredictionResponse response = DelayPredictionResponse.builder()
                .trainNumber("IC456")
                .stationCode("DEB")
                .predictedDelay(7d)
                .build();

        when(webClient.makeDeparturePredictionRequest(request))
                .thenReturn(Mono.just(response));

        Mono<DelayPredictionResponse> result = testedObject.getDepartureDelay(request);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        verify(webClient).makeDeparturePredictionRequest(request);
    }

    @Test
    void getDepartureDelay_requestNotPermitted_returnsDefaultResponse() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC456")
                .stationCode("DEB")
                .build();

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("getDepartureDelay");
        RequestNotPermitted exception =
                RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        when(webClient.makeDeparturePredictionRequest(request))
                .thenReturn(Mono.error(exception));

        Mono<DelayPredictionResponse> result = testedObject.getDepartureDelay(request);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("IC456", resp.getTrainNumber());
                    assertEquals("DEB", resp.getStationCode());
                    assertEquals(0d, resp.getPredictedDelay());
                })
                .verifyComplete();

        verify(webClient).makeDeparturePredictionRequest(request);
    }

    @Test
    void getDepartureDelay_callNotPermitted_returnsDefaultResponse() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC456")
                .stationCode("DEB")
                .build();

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("getDepartureDelay");
        CallNotPermittedException exception =
                CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(webClient.makeDeparturePredictionRequest(request))
                .thenReturn(Mono.error(exception));

        Mono<DelayPredictionResponse> result = testedObject.getDepartureDelay(request);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("IC456", resp.getTrainNumber());
                    assertEquals("DEB", resp.getStationCode());
                    assertEquals(0d, resp.getPredictedDelay());
                })
                .verifyComplete();

        verify(webClient).makeDeparturePredictionRequest(request);
    }

    @Test
    void getDepartureDelay_otherException_returnsDefaultResponse() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC456")
                .stationCode("DEB")
                .build();

        RuntimeException exception = new RuntimeException("boom");

        when(webClient.makeDeparturePredictionRequest(request))
                .thenReturn(Mono.error(exception));

        Mono<DelayPredictionResponse> result = testedObject.getDepartureDelay(request);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("IC456", resp.getTrainNumber());
                    assertEquals("DEB", resp.getStationCode());
                    assertEquals(0d, resp.getPredictedDelay());
                })
                .verifyComplete();

        verify(webClient).makeDeparturePredictionRequest(request);
    }
}
