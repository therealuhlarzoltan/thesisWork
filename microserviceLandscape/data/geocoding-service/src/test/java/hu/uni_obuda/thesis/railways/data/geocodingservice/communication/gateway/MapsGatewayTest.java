package hu.uni_obuda.thesis.railways.data.geocodingservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.client.MapsWebClient;
import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.response.CoordinatesResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpRequest;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MapsGatewayTest {

    @Mock
    private MapsWebClient webClient;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private RetryRegistry retryRegistry;
    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    private MapsGateway testedObject;

    @BeforeEach
    public void setUp() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("geocodingApi");
        Retry retry = Retry.ofDefaults("geocodingApi");
        RateLimiter rl = RateLimiter.ofDefaults("geocodingApi");

        when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(cb);
        when(retryRegistry.retry(anyString())).thenReturn(retry);
        when(rateLimiterRegistry.rateLimiter(anyString())).thenReturn(rl);

        testedObject = new MapsGatewayImpl(
                webClient,
                circuitBreakerRegistry,
                retryRegistry,
                rateLimiterRegistry
        );
    }

    @Test
    public void getCoordinates_whenUpstreamSucceeds_thenPassesThrough() {
        CoordinatesResponse response = new CoordinatesResponse();

        when(webClient.getCoordinates("ADDR"))
                .thenReturn(Mono.just(response));

        StepVerifier.create(testedObject.getCoordinates("ADDR"))
                .expectNext(response)
                .verifyComplete();

        verify(webClient).getCoordinates("ADDR");
    }

    @Test
    public void getCoordinates_whenWebClientResponseException_thenMapsToExternalApiException() {
        WebClientResponseException responseException = createWebClientResponseException(
                HttpStatus.BAD_GATEWAY,
                "https://external.example.com/geocode"
        );

        when(webClient.getCoordinates("ADDR"))
                .thenReturn(Mono.error(responseException));

        StepVerifier.create(testedObject.getCoordinates("ADDR"))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isInstanceOf(ExternalApiException.class)
                )
                .verify();
    }

    @Test
    public void getCoordinates_whenWebClientRequestException_thenMapsToInternalApiException() {
        WebClientRequestException requestException = new WebClientRequestException(
                new RuntimeException("IO error"),
                HttpMethod.GET,
                URI.create("https://external.example.com/geocode"),
                new HttpHeaders()
        );

        when(webClient.getCoordinates("ADDR"))
                .thenReturn(Mono.error(requestException));

        StepVerifier.create(testedObject.getCoordinates("ADDR"))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isInstanceOf(InternalApiException.class)
                )
                .verify();
    }

    @Test
    public void getCoordinates_whenExternalApiException_thenSameInstanceIsPropagated() throws Exception {
        ExternalApiException external = new ExternalApiException(
                HttpStatusCode.valueOf(502),
                new URL("https://external.example.com/fail")
        );

        when(webClient.getCoordinates("ADDR"))
                .thenReturn(Mono.error(external));

        StepVerifier.create(testedObject.getCoordinates("ADDR"))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isSameAs(external)
                )
                .verify();
    }

    @Test
    public void getCoordinates_whenInternalApiException_thenSameInstanceIsPropagated() throws Exception {
        InternalApiException internal = new InternalApiException(
                "internal error",
                new URL("https://internal.example.com")
        );

        when(webClient.getCoordinates("ADDR"))
                .thenReturn(Mono.error(internal));

        StepVerifier.create(testedObject.getCoordinates("ADDR"))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isSameAs(internal)
                )
                .verify();
    }

    @Test
    public void getCoordinates_whenRequestNotPermitted_thenMapsToInternalApiException() {
        RequestNotPermitted requestNotPermitted = mock(RequestNotPermitted.class);

        when(webClient.getCoordinates("ADDR"))
                .thenReturn(Mono.error(requestNotPermitted));

        StepVerifier.create(testedObject.getCoordinates("ADDR"))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isInstanceOf(InternalApiException.class)
                )
                .verify();
    }

    @Test
    public void getCoordinates_whenCallNotPermitted_thenMapsToInternalApiException() {
        CallNotPermittedException callNotPermitted = mock(CallNotPermittedException.class);

        when(webClient.getCoordinates("ADDR"))
                .thenReturn(Mono.error(callNotPermitted));

        StepVerifier.create(testedObject.getCoordinates("ADDR"))
                .expectErrorSatisfies(throwable ->
                        assertThat(throwable).isInstanceOf(InternalApiException.class)
                )
                .verify();
    }

    @Test
    public void getCoordinates_whenRuntimeException_thenMapsToInternalApiException() {
        RuntimeException runtime = new RuntimeException("boom");

        when(webClient.getCoordinates("ADDR"))
                .thenReturn(Mono.error(runtime));

        StepVerifier.create(testedObject.getCoordinates("ADDR"))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(InternalApiException.class);
                    ApiException apiException = (ApiException) throwable;
                    assertThat(apiException.getMessage()).contains("A runtime exception occurred");
                })
                .verify();
    }

    private WebClientResponseException createWebClientResponseException(HttpStatus status, String url) {
        HttpHeaders headers = new HttpHeaders();

        HttpRequest request = new HttpRequest() {
            @Override
            public HttpMethod getMethod() {
                return HttpMethod.GET;
            }

            @Override
            public URI getURI() {
                return URI.create(url);
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Map.of();
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };

        return WebClientResponseException.create(
                status.value(),
                status.getReasonPhrase(),
                headers,
                new byte[0],
                StandardCharsets.UTF_8,
                request
        );
    }
}
