package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveGeocodingWebClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ReactiveGeocodingWebClient testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveGeocodingWebClient(webClient);

        ReflectionTestUtils.setField(testedObject, "baseUrl", "http://geocoder-host");
        ReflectionTestUtils.setField(testedObject, "geocodingUri", "/api/geocode");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void makeGeocodingRequest_success_returnsMonoOfGeocodingResponse() {
        String stationName = "Budapest-Keleti";

        GeocodingResponse response = GeocodingResponse.builder()
                .latitude(47.5000)
                .longitude(19.0833)
                .address("Budapest, Keleti pályaudvar")
                .build();

        when(responseSpec.bodyToMono(GeocodingResponse.class))
                .thenReturn(Mono.just(response));

        Mono<GeocodingResponse> result = testedObject.makeGeocodingRequest(stationName);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue();

        assertTrue(uri.startsWith("http://geocoder-host/api/geocode"));
        assertTrue(uri.contains("address=Budapest-Keleti"));

        verify(webClient).get();
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(GeocodingResponse.class);
    }

    @Test
    void makeGeocodingRequest_deserializationError_propagatesError() {
        String stationName = "Budapest-Keleti";

        RuntimeException decodeError = new RuntimeException("Decode error");
        when(responseSpec.bodyToMono(GeocodingResponse.class))
                .thenReturn(Mono.error(decodeError));

        Mono<GeocodingResponse> result = testedObject.makeGeocodingRequest(stationName);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertEquals("Decode error", ex.getMessage()))
                .verify();

        verify(webClient).get();
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(GeocodingResponse.class);
    }
}
