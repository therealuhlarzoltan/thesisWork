package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client.WeatherDataWebClient;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherDataGatewayTest {

    @Mock
    private WeatherDataWebClient weatherDataClient;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private RetryRegistry retryRegistry;
    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    private WeatherDataGatewayImpl testedObject;

    @BeforeEach
    void setUp() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("getWeatherDataApi");
        Retry retry = Retry.ofDefaults("getWeatherDataApi");
        RateLimiter rateLimiter = RateLimiter.ofDefaults("getWeatherDataApi");

        when(circuitBreakerRegistry.circuitBreaker("getWeatherDataApi")).thenReturn(circuitBreaker);
        when(retryRegistry.retry("getWeatherDataApi")).thenReturn(retry);
        when(rateLimiterRegistry.rateLimiter("getWeatherDataApi")).thenReturn(rateLimiter);

        testedObject = new WeatherDataGatewayImpl(
                weatherDataClient,
                circuitBreakerRegistry,
                retryRegistry,
                rateLimiterRegistry
        );
    }

    @Test
    void getWeatherByCoordinates_whenUpstreamSucceeds_thenPassesThrough() {
        LocalDate date = LocalDate.of(2024, 10, 10);
        WeatherResponse response = WeatherResponse.builder()
                .latitude(47.0)
                .longitude(19.0)
                .build();

        when(weatherDataClient.getWeatherByCoordinates(47.0, 19.0, date))
                .thenReturn(Mono.just(response));

        StepVerifier.create(testedObject.getWeatherByCoordinates(47.0, 19.0, date))
                .expectNext(response)
                .verifyComplete();

        verify(weatherDataClient).getWeatherByCoordinates(47.0, 19.0, date);
    }

    @Test
    void getWeatherByCoordinates_whenCallNotPermitted_thenReturnsFallbackWeatherResponse() {
        LocalDate date = LocalDate.of(2024, 10, 10);
        CallNotPermittedException callNotPermitted = mock(CallNotPermittedException.class);

        when(weatherDataClient.getWeatherByCoordinates(47.0, 19.0, date))
                .thenReturn(Mono.error(callNotPermitted));

        WeatherResponse expectedFallback = WeatherResponse.builder().build();

        StepVerifier.create(testedObject.getWeatherByCoordinates(47.0, 19.0, date))
                .assertNext(actual -> assertThat(actual).isEqualTo(expectedFallback))
                .verifyComplete();
    }

    @Test
    void getWeatherByCoordinates_whenRequestNotPermitted_thenReturnsFallbackWeatherResponse() {
        LocalDate date = LocalDate.of(2024, 10, 10);
        RequestNotPermitted requestNotPermitted = mock(RequestNotPermitted.class);

        when(weatherDataClient.getWeatherByCoordinates(47.0, 19.0, date))
                .thenReturn(Mono.error(requestNotPermitted));

        WeatherResponse expectedFallback = WeatherResponse.builder().build();

        StepVerifier.create(testedObject.getWeatherByCoordinates(47.0, 19.0, date))
                .assertNext(actual -> assertThat(actual).isEqualTo(expectedFallback))
                .verifyComplete();
    }

    @Test
    void getWeatherByCoordinates_whenOtherException_thenReturnsFallbackWeatherResponse() {
        LocalDate date = LocalDate.of(2024, 10, 10);
        RuntimeException runtime = new RuntimeException("exception");

        when(weatherDataClient.getWeatherByCoordinates(47.0, 19.0, date))
                .thenReturn(Mono.error(runtime));

        WeatherResponse expectedFallback = WeatherResponse.builder().build();

        StepVerifier.create(testedObject.getWeatherByCoordinates(47.0, 19.0, date))
                .assertNext(actual -> assertThat(actual).isEqualTo(expectedFallback))
                .verifyComplete();
    }
}
