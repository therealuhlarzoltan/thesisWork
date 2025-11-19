package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client.ElviraRailDataWebClient;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
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
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ElviraRailDataGatewayTest {

    @Mock
    private ElviraRailDataWebClient webClient;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    private ElviraRailDataGateway testedObject;

    @BeforeEach
    void setUp() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("cb");
        Retry retry = Retry.ofDefaults("retry");
        RateLimiter rateLimiter = RateLimiter.ofDefaults("rl");

        when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        when(retryRegistry.retry(anyString())).thenReturn(retry);
        when(rateLimiterRegistry.rateLimiter(anyString())).thenReturn(rateLimiter);

        testedObject = new ElviraRailDataGatewayImpl(webClient, circuitBreakerRegistry, retryRegistry, rateLimiterRegistry);
    }

    @Test
    void getShortTimetable_whenUpstreamSucceeds_thenPassesThrough() {
        LocalDate date = LocalDate.of(2024, 10, 10);
        ElviraShortTimetableResponse response = new ElviraShortTimetableResponse();

        when(webClient.getShortTimetable("FROM", "TO", date))
                .thenReturn(Mono.just(response));

        Mono<ElviraShortTimetableResponse> result =
                testedObject.getShortTimetable("FROM", "TO", date);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        verify(webClient).getShortTimetable("FROM", "TO", date);
    }

    @Test
    void getShortTrainDetails_whenUpstreamSucceeds_thenPassesThrough() {
        ElviraShortTrainDetailsResponse response = new ElviraShortTrainDetailsResponse();
        when(webClient.getShortTrainDetails("uri"))
                .thenReturn(Mono.just(response));

        StepVerifier.create(testedObject.getShortTrainDetails("uri"))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void getTimetable_whenUpstreamSucceeds_thenPassesThrough() {
        LocalDate date = LocalDate.of(2024, 10, 10);
        ElviraTimetableResponse response = new ElviraTimetableResponse();

        when(webClient.getTimetable("FROM", "TO", date))
                .thenReturn(Mono.just(response));

        StepVerifier.create(testedObject.getTimetable("FROM", "TO", date))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void getShortTimetable_whenWebClientResponseException_thenMapsToExternalApiException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        WebClientResponseException responseException = createWebClientResponseException(
                HttpStatus.BAD_GATEWAY,
                "https://external.example.com/api"
        );

        when(webClient.getShortTimetable("FROM", "TO", date))
                .thenReturn(Mono.error(responseException));

        StepVerifier.create(testedObject.getShortTimetable("FROM", "TO", date))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ExternalApiException.class);
                })
                .verify();
    }

    @Test
    void getShortTimetable_whenWebClientRequestException_thenMapsToInternalApiException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        WebClientRequestException requestException = new WebClientRequestException(
                new RuntimeException("IO error"),
                HttpMethod.GET,
                URI.create("https://external.example.com/api"),
                new HttpHeaders()
        );

        when(webClient.getShortTimetable("FROM", "TO", date))
                .thenReturn(Mono.error(requestException));

        StepVerifier.create(testedObject.getShortTimetable("FROM", "TO", date))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(InternalApiException.class);
                })
                .verify();
    }

    @Test
    void getShortTimetable_whenExternalApiException_thenSameInstanceIsPropagated() throws Exception {
        LocalDate date = LocalDate.of(2024, 10, 10);

        ExternalApiException external = new ExternalApiException(
                HttpStatusCode.valueOf(502),
                new URL("https://external.example.com/fail")
        );

        when(webClient.getShortTimetable("FROM", "TO", date))
                .thenReturn(Mono.error(external));

        StepVerifier.create(testedObject.getShortTimetable("FROM", "TO", date))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isSameAs(external);
                })
                .verify();
    }

    @Test
    void getShortTimetable_whenInternalApiException_thenSameInstanceIsPropagated() throws Exception {
        LocalDate date = LocalDate.of(2024, 10, 10);

        InternalApiException internal = new InternalApiException(
                "internal error",
                new URL("https://internal.example.com")
        );

        when(webClient.getShortTimetable("FROM", "TO", date))
                .thenReturn(Mono.error(internal));

        StepVerifier.create(testedObject.getShortTimetable("FROM", "TO", date))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isSameAs(internal);
                })
                .verify();
    }

    @Test
    void getShortTimetable_whenRequestNotPermitted_thenMapsToInternalApiException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        RequestNotPermitted requestNotPermitted = mock(RequestNotPermitted.class);

        when(webClient.getShortTimetable("FROM", "TO", date))
                .thenReturn(Mono.error(requestNotPermitted));

        StepVerifier.create(testedObject.getShortTimetable("FROM", "TO", date))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(InternalApiException.class);
                })
                .verify();
    }

    @Test
    void getShortTimetable_whenCallNotPermitted_thenMapsToInternalApiException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        CallNotPermittedException callNotPermitted = mock(CallNotPermittedException.class);

        when(webClient.getShortTimetable("FROM", "TO", date))
                .thenReturn(Mono.error(callNotPermitted));

        StepVerifier.create(testedObject.getShortTimetable("FROM", "TO", date))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(InternalApiException.class);
                })
                .verify();
    }

    @Test
    void getShortTimetable_whenRuntimeException_thenMapsToInternalApiException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        RuntimeException runtime = new RuntimeException("boom");

        when(webClient.getShortTimetable("FROM", "TO", date))
                .thenReturn(Mono.error(runtime));

        StepVerifier.create(testedObject.getShortTimetable("FROM", "TO", date))
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