package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactivePredictorWebClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ReactivePredictorWebClient testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactivePredictorWebClient(webClient);

        ReflectionTestUtils.setField(testedObject, "baseUrl", "http://predictor-host");
        ReflectionTestUtils.setField(testedObject, "arrivalUri", "/api/arrival");
        ReflectionTestUtils.setField(testedObject, "departureUri", "/api/departure");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void makeArrivalPredictionRequest_success_returnsMonoOfDelayPredictionResponse() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .build();

        DelayPredictionResponse response = DelayPredictionResponse.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .predictedDelay(12d)
                .build();

        when(responseSpec.bodyToMono(DelayPredictionResponse.class))
                .thenReturn(Mono.just(response));

        Mono<DelayPredictionResponse> result = testedObject.makeArrivalPredictionRequest(request);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodyUriSpec).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue();

        assertEquals("http://predictor-host/api/arrival", uri);
        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(request);
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(DelayPredictionResponse.class);
    }

    @Test
    void makeArrivalPredictionRequest_deserializationError_propagatesError() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .build();

        RuntimeException decodeError = new RuntimeException("Decode error");
        when(responseSpec.bodyToMono(DelayPredictionResponse.class))
                .thenReturn(Mono.error(decodeError));

        Mono<DelayPredictionResponse> result = testedObject.makeArrivalPredictionRequest(request);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertEquals("Decode error", ex.getMessage()))
                .verify();

        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(request);
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(DelayPredictionResponse.class);
    }

    @Test
    void makeDeparturePredictionRequest_success_returnsMonoOfDelayPredictionResponse() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC456")
                .stationCode("DEB")
                .build();

        DelayPredictionResponse response = DelayPredictionResponse.builder()
                .trainNumber("IC456")
                .stationCode("DEB")
                .predictedDelay(7d)
                .build();

        when(responseSpec.bodyToMono(DelayPredictionResponse.class))
                .thenReturn(Mono.just(response));

        Mono<DelayPredictionResponse> result = testedObject.makeDeparturePredictionRequest(request);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodyUriSpec, atLeastOnce()).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue();

        assertEquals("http://predictor-host/api/departure", uri);
        verify(webClient, atLeastOnce()).post();
        verify(requestBodyUriSpec).bodyValue(request);
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(DelayPredictionResponse.class);
    }

    @Test
    void makeDeparturePredictionRequest_deserializationError_propagatesError() {
        DelayPredictionRequest request = DelayPredictionRequest.builder()
                .trainNumber("IC456")
                .stationCode("DEB")
                .build();

        RuntimeException decodeError = new RuntimeException("Decode error");
        when(responseSpec.bodyToMono(DelayPredictionResponse.class))
                .thenReturn(Mono.error(decodeError));

        Mono<DelayPredictionResponse> result = testedObject.makeDeparturePredictionRequest(request);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertEquals("Decode error", ex.getMessage()))
                .verify();

        verify(webClient, atLeastOnce()).post();
        verify(requestBodyUriSpec).bodyValue(request);
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(DelayPredictionResponse.class);
    }
}
