package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveElviraRailWebClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    private ReactiveElviraRailWebClient testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveElviraRailWebClient(webClient);

        ReflectionTestUtils.setField(testedObject, "baseUrl", "http://test-host");
        ReflectionTestUtils.setField(testedObject, "routePlannerUri", "/api/routes");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
    }

    @Test
    void makeRouteRequest_success_returnsFluxOfTrainRouteResponse() {
        String from = "BP";
        String to = "DEB";
        LocalDate date = LocalDate.of(2025, 1, 1);

        TrainRouteResponse route1 = TrainRouteResponse.builder()
                .trains(Collections.emptyList())
                .build();
        TrainRouteResponse route2 = TrainRouteResponse.builder()
                .trains(Collections.emptyList())
                .build();

        when(requestHeadersSpec.exchangeToFlux(any()))
                .thenReturn(Flux.just(route1, route2));

        Flux<TrainRouteResponse> result = testedObject.makeRouteRequest(from, to, date);

        StepVerifier.create(result)
                .expectNext(route1, route2)
                .verifyComplete();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue();
        assertTrue(uri.startsWith("http://test-host/api/routes"));
        assertTrue(uri.contains("from=BP"));
        assertTrue(uri.contains("to=DEB"));
        assertTrue(uri.contains("date=2025-01-01"));

        verify(webClient).get();
    }

    @Test
    void makeRouteRequest_deserializationError_propagatesError() {
        String from = "BP";
        String to = "DEB";
        LocalDate date = LocalDate.of(2025, 1, 1);

        RuntimeException decodeError = new RuntimeException("Decode error");
        when(requestHeadersSpec.exchangeToFlux(any()))
                .thenReturn(Flux.error(decodeError));

        Flux<TrainRouteResponse> result = testedObject.makeRouteRequest(from, to, date);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertEquals("Decode error", ex.getMessage()))
                .verify();

        verify(webClient).get();
    }

    @Test
    void makeRouteRequest_404_returnsEntityNotFoundException() {
        String from = "BP";
        String to = "DEB";
        LocalDate date = LocalDate.of(2025, 1, 1);

        when(requestHeadersSpec.exchangeToFlux(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Flux<TrainRouteResponse>> mapper =
                            (Function<ClientResponse, Flux<TrainRouteResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(404));

                    HttpRequest httpRequest = mock(HttpRequest.class);
                    when(httpRequest.getURI()).thenReturn(URI.create("http://test-host/api/routes"));
                    when(clientResponse.request()).thenReturn(httpRequest);

                    return mapper.apply(clientResponse);
                });

        Flux<TrainRouteResponse> result = testedObject.makeRouteRequest(from, to, date);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(EntityNotFoundException.class, ex))
                .verify();
    }

    @Test
    void makeRouteRequest_non404Error_returnsExternalApiException() {
        String from = "BP";
        String to = "DEB";
        LocalDate date = LocalDate.of(2025, 1, 1);

        when(requestHeadersSpec.exchangeToFlux(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Flux<TrainRouteResponse>> mapper =
                            (Function<ClientResponse, Flux<TrainRouteResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

                    HttpRequest httpRequest = mock(HttpRequest.class);
                    when(httpRequest.getURI()).thenReturn(URI.create("http://test-host/api/routes"));
                    when(clientResponse.request()).thenReturn((httpRequest));

                    return mapper.apply(clientResponse);
                });

        Flux<TrainRouteResponse> result = testedObject.makeRouteRequest(from, to, date);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(ExternalApiException.class, ex))
                .verify();
    }
}
