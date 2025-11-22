package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.ElviraRailWebClient;
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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveElviraRailDataGatewayTest {

    @Mock
    private ElviraRailWebClient webClient;

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

    private ReactiveElviraRailDataGateway testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveElviraRailDataGateway(
                webClient,
                circuitBreakerRegistry,
                retryRegistry,
                rateLimiterRegistry
        );
    }

    @Test
    void getTimetable_success_returnsClientFlux() {
        String from = "BP";
        String to = "DEB";
        LocalDate date = LocalDate.now();

        TrainRouteResponse route1 = TrainRouteResponse.builder()
                .trains(Collections.emptyList())
                .build();
        TrainRouteResponse route2 = TrainRouteResponse.builder()
                .trains(Collections.emptyList())
                .build();

        when(webClient.makeRouteRequest(from, to, date))
                .thenReturn(Flux.just(route1, route2));

        Flux<TrainRouteResponse> result = testedObject.getTimetable(from, to, date);

        StepVerifier.create(result)
                .expectNext(route1, route2)
                .verifyComplete();

        verify(webClient).makeRouteRequest(from, to, date);
    }

    @Test
    void getTimetable_requestNotPermitted_returnsEmptyFlux() {
        String from = "BP";
        String to = "DEB";
        LocalDate date = LocalDate.now();

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("getTimetable");
        RequestNotPermitted exception =
                RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        when(webClient.makeRouteRequest(from, to, date))
                .thenReturn(Flux.error(exception));

        Flux<TrainRouteResponse> result = testedObject.getTimetable(from, to, date);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeRouteRequest(from, to, date);
    }

    @Test
    void getTimetable_callNotPermitted_returnsEmptyFlux() {
        String from = "BP";
        String to = "DEB";
        LocalDate date = LocalDate.now();

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("getTimetable");
        CallNotPermittedException exception =
                CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(webClient.makeRouteRequest(from, to, date))
                .thenReturn(Flux.error(exception));

        Flux<TrainRouteResponse> result = testedObject.getTimetable(from, to, date);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeRouteRequest(from, to, date);
    }

    @Test
    void getTimetable_otherException_returnsEmptyFlux() {
        // given
        String from = "BP";
        String to = "DEB";
        LocalDate date = LocalDate.now();

        RuntimeException exception = new RuntimeException("boom");

        when(webClient.makeRouteRequest(from, to, date))
                .thenReturn(Flux.error(exception));

        Flux<TrainRouteResponse> result = testedObject.getTimetable(from, to, date);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(webClient).makeRouteRequest(from, to, date);
    }
}
