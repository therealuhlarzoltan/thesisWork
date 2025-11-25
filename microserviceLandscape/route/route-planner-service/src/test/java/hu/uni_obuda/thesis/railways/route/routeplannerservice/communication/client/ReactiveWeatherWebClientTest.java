package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveWeatherWebClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ReactiveWeatherWebClient testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveWeatherWebClient(webClient);

        ReflectionTestUtils.setField(testedObject, "baseUrl", "http://weather-host");
        ReflectionTestUtils.setField(testedObject, "weatherUri", "/api/weather");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void makeWeatherRequest_success_returnsMonoOfWeatherInfo() {
        String address = "Budapest";
        Double latitude = 47.4979;
        Double longitude = 19.0402;
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 1, 10, 30);

        WeatherInfo response = WeatherInfo.builder()
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .time(dateTime)
                .temperature(20.5)
                .build();

        when(responseSpec.bodyToMono(WeatherInfo.class))
                .thenReturn(Mono.just(response));

        Mono<WeatherInfo> result =
                testedObject.makeWeatherRequest(address, latitude, longitude, dateTime);

        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue();

        assertTrue(uri.startsWith("http://weather-host/api/weather"));
        assertTrue(uri.contains("stationName=Budapest"));
        assertTrue(uri.contains("latitude=47.4979"));
        assertTrue(uri.contains("longitude=19.0402"));
        assertTrue(uri.contains("dateTime=2025-01-01T10:30"));

        verify(webClient).get();
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(WeatherInfo.class);
    }

    @Test
    void makeWeatherRequest_deserializationError_propagatesError() {
        String address = "Budapest";
        Double latitude = 47.4979;
        Double longitude = 19.0402;
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 1, 10, 30);

        RuntimeException decodeError = new RuntimeException("Decode error");
        when(responseSpec.bodyToMono(WeatherInfo.class))
                .thenReturn(Mono.error(decodeError));

        Mono<WeatherInfo> result =
                testedObject.makeWeatherRequest(address, latitude, longitude, dateTime);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> assertEquals("Decode error", ex.getMessage()))
                .verify();

        verify(webClient).get();
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(WeatherInfo.class);
    }
}
