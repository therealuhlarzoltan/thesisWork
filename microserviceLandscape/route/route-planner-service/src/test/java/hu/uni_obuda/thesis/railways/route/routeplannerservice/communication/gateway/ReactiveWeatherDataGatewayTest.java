package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.WeatherWebClient;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveWeatherDataGatewayTest {

    @Mock
    private WeatherWebClient webClient;

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

    private ReactiveWeatherDataGateway testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveWeatherDataGateway(
                webClient,
                circuitBreakerRegistry,
                retryRegistry,
                rateLimiterRegistry
        );
    }

    @Test
    void getWeatherInfo_success_returnsClientMono() {
        String address = "Budapest";
        Double latitude = 47.4979;
        Double longitude = 19.0402;
        LocalDateTime time = LocalDateTime.now();

        WeatherInfo response = WeatherInfo.builder()
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .time(time)
                .temperature(20.5)
                .relativeHumidity(60.0)
                .build();

        when(webClient.makeWeatherRequest(address, latitude, longitude, time))
                .thenReturn(Mono.just(response));

        Mono<WeatherInfo> result =
                testedObject.getWeatherInfo(address, latitude, longitude, time);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        verify(webClient).makeWeatherRequest(address, latitude, longitude, time);
    }

    @Test
    void getWeatherInfo_requestNotPermitted_returnsDefaultResponse() {
        String address = "Budapest";
        Double latitude = 47.4979;
        Double longitude = 19.0402;
        LocalDateTime time = LocalDateTime.now();

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("getWeatherInfo");
        RequestNotPermitted exception =
                RequestNotPermitted.createRequestNotPermitted(rateLimiter);

        when(webClient.makeWeatherRequest(address, latitude, longitude, time))
                .thenReturn(Mono.error(exception));

        Mono<WeatherInfo> result =
                testedObject.getWeatherInfo(address, latitude, longitude, time);

        StepVerifier.create(result)
                .assertNext(info -> {
                    assertEquals(address, info.getAddress());
                    assertEquals(latitude, info.getLatitude());
                    assertEquals(longitude, info.getLongitude());
                    assertEquals(time, info.getTime());
                    assertNull(info.getTemperature());
                    assertNull(info.getRelativeHumidity());
                })
                .verifyComplete();

        verify(webClient).makeWeatherRequest(address, latitude, longitude, time);
    }

    @Test
    void getWeatherInfo_callNotPermitted_returnsDefaultResponse() {
        String address = "Budapest";
        Double latitude = 47.4979;
        Double longitude = 19.0402;
        LocalDateTime time = LocalDateTime.now();

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("getWeatherInfo");
        CallNotPermittedException exception =
                CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

        when(webClient.makeWeatherRequest(address, latitude, longitude, time))
                .thenReturn(Mono.error(exception));

        Mono<WeatherInfo> result =
                testedObject.getWeatherInfo(address, latitude, longitude, time);

        StepVerifier.create(result)
                .assertNext(info -> {
                    assertEquals(address, info.getAddress());
                    assertEquals(latitude, info.getLatitude());
                    assertEquals(longitude, info.getLongitude());
                    assertEquals(time, info.getTime());
                    assertNull(info.getTemperature());
                    assertNull(info.getRelativeHumidity());
                })
                .verifyComplete();

        verify(webClient).makeWeatherRequest(address, latitude, longitude, time);
    }

    @Test
    void getWeatherInfo_otherException_returnsDefaultResponse() {
        String address = "Budapest";
        Double latitude = 47.4979;
        Double longitude = 19.0402;
        LocalDateTime time = LocalDateTime.now();

        RuntimeException exception = new RuntimeException("boom");

        when(webClient.makeWeatherRequest(address, latitude, longitude, time))
                .thenReturn(Mono.error(exception));

        Mono<WeatherInfo> result =
                testedObject.getWeatherInfo(address, latitude, longitude, time);

        StepVerifier.create(result)
                .assertNext(info -> {
                    assertEquals(address, info.getAddress());
                    assertEquals(latitude, info.getLatitude());
                    assertEquals(longitude, info.getLongitude());
                    assertEquals(time, info.getTime());
                    assertNull(info.getTemperature());
                    assertNull(info.getRelativeHumidity());
                })
                .verifyComplete();

        verify(webClient).makeWeatherRequest(address, latitude, longitude, time);
    }
}
