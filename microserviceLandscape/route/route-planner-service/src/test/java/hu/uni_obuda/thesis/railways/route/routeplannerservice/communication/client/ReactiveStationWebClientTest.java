package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveStationWebClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    private ReactiveStationWebClient testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveStationWebClient(webClient, new ObjectMapper());

        ReflectionTestUtils.setField(testedObject, "baseUrl", "http://delay-host");
        ReflectionTestUtils.setField(testedObject, "trainRouteUri", "/api/route");
        ReflectionTestUtils.setField(testedObject, "trainStationUri", "/api/station");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
    }

    @Test
    void makeStationRequest_success_returnsFirstElement() {
        String stationCode = "BPK";

        String json = """
                [
                  {"stationCode":"BPK","latitude":47.0,"longitude":19.0},
                  {"stationCode":"OTHER","latitude":48.0,"longitude":20.0}
                ]
                """;

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainStationResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainStationResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
                    when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just(json));

                    return mapper.apply(clientResponse);
                });

        Mono<TrainStationResponse> result = testedObject.makeStationRequest(stationCode);

        TrainStationResponse expected = TrainStationResponse.builder()
                .stationCode("BPK")
                .latitude(47.0)
                .longitude(19.0)
                .build();

        StepVerifier.create(result)
                .expectNext(expected)
                .verifyComplete();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue();

        assertTrue(uri.startsWith("http://delay-host/api/station"));
        assertTrue(uri.contains("stationCode=BPK"));
        verify(webClient).get();
    }

    @Test
    void makeStationRequest_emptyList_returnsEntityNotFound() {
        String stationCode = "BPK";

        String json = "[]";

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainStationResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainStationResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
                    when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just(json));

                    return mapper.apply(clientResponse);
                });

        Mono<TrainStationResponse> result = testedObject.makeStationRequest(stationCode);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(EntityNotFoundException.class, ex))
                .verify();
    }

    @Test
    void makeStationRequest_invalidJson_returnsExternalApiFormatMismatchException() {
        String stationCode = "BPK";

        String invalidJson = "not-a-json";

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainStationResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainStationResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
                    when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just(invalidJson));

                    return mapper.apply(clientResponse);
                });

        Mono<TrainStationResponse> result = testedObject.makeStationRequest(stationCode);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(ExternalApiFormatMismatchException.class, ex))
                .verify();
    }

    @Test
    void makeStationRequest_404_returnsEntityNotFoundException() {
        String stationCode = "BPK";

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainStationResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainStationResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(404));

                    return mapper.apply(clientResponse);
                });

        Mono<TrainStationResponse> result = testedObject.makeStationRequest(stationCode);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(EntityNotFoundException.class, ex))
                .verify();
    }

    @Test
    void makeStationRequest_non404_returnsExternalApiException() {
        String stationCode = "BPK";

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainStationResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainStationResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

                    HttpRequest httpRequest = mock(HttpRequest.class);
                    when(httpRequest.getURI()).thenReturn(URI.create("http://delay-host/api/station?stationCode=BPK"));
                    when(clientResponse.request()).thenReturn(httpRequest);

                    return mapper.apply(clientResponse);
                });

        Mono<TrainStationResponse> result = testedObject.makeStationRequest(stationCode);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(ExternalApiException.class, ex))
                .verify();
    }

    @Test
    void makeTrainRouteRequest_success_returnsFirstElement() {
        String trainNumber = "IC123";

        String json = """
                [
                  {"trainNumber":"IC123","lineNumber":"80","startStation":"Budapest-Keleti","endStation":"Miskolc-Tiszai"},
                  {"trainNumber":"IC124","lineNumber":"80","startStation":"Budapest-Keleti","endStation":"Miskolc-Tiszai"}
                ]
                """;

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainRouteResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainRouteResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
                    when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just(json));

                    return mapper.apply(clientResponse);
                });

        Mono<TrainRouteResponse> result = testedObject.makeTrainRouteRequest(trainNumber);

        TrainRouteResponse expected = new TrainRouteResponse(
                "IC123",
                "80",
                "Budapest-Keleti",
                "Miskolc-Tiszai"
        );

        StepVerifier.create(result)
                .expectNext(expected)
                .verifyComplete();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue();

        assertTrue(uri.startsWith("http://delay-host/api/route"));
        assertTrue(uri.contains("trainNumber=IC123"));
        verify(webClient, atLeastOnce()).get();
    }

    @Test
    void makeTrainRouteRequest_emptyList_returnsEntityNotFound() {
        String trainNumber = "IC123";

        String json = "[]";

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainRouteResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainRouteResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
                    when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just(json));

                    return mapper.apply(clientResponse);
                });

        Mono<TrainRouteResponse> result = testedObject.makeTrainRouteRequest(trainNumber);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(EntityNotFoundException.class, ex))
                .verify();
    }

    @Test
    void makeTrainRouteRequest_invalidJson_returnsExternalApiFormatMismatchException() {
        String trainNumber = "IC123";

        String invalidJson = "not-a-json";

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainRouteResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainRouteResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
                    when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just(invalidJson));

                    return mapper.apply(clientResponse);
                });

        Mono<TrainRouteResponse> result = testedObject.makeTrainRouteRequest(trainNumber);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(ExternalApiFormatMismatchException.class, ex))
                .verify();
    }

    @Test
    void makeTrainRouteRequest_404_returnsEntityNotFoundException() {
        String trainNumber = "IC123";

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainRouteResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainRouteResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(404));

                    return mapper.apply(clientResponse);
                });

        Mono<TrainRouteResponse> result = testedObject.makeTrainRouteRequest(trainNumber);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(EntityNotFoundException.class, ex))
                .verify();
    }

    @Test
    void makeTrainRouteRequest_non404_returnsExternalApiException() {
        String trainNumber = "IC123";

        when(requestHeadersSpec.exchangeToMono(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Function<ClientResponse, Mono<TrainRouteResponse>> mapper =
                            (Function<ClientResponse, Mono<TrainRouteResponse>>) invocation.getArgument(0);

                    ClientResponse clientResponse = mock(ClientResponse.class);
                    when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

                    HttpRequest httpRequest = mock(HttpRequest.class);
                    when(httpRequest.getURI()).thenReturn(URI.create("http://delay-host/api/route?trainNumber=IC123"));
                    when(clientResponse.request()).thenReturn(httpRequest);

                    return mapper.apply(clientResponse);
                });

        Mono<TrainRouteResponse> result = testedObject.makeTrainRouteRequest(trainNumber);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertInstanceOf(ExternalApiException.class, ex))
                .verify();
    }
}
