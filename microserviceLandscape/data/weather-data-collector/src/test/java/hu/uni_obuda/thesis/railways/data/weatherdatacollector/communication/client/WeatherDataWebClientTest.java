package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherDataWebClientTest {

    private static final String BASE_URL = "https://weather.example.com";

    @Mock
    private ExchangeFunction exchangeFunction;

    private WeatherDataWebClientImpl testedObject;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .exchangeFunction(exchangeFunction)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        testedObject = new WeatherDataWebClientImpl(webClient, objectMapper);

        ReflectionTestUtils.setField(testedObject, "weatherBaseUrl", BASE_URL);
        ReflectionTestUtils.setField(testedObject, "forecastUri", "/v1/forecast");
        ReflectionTestUtils.setField(testedObject, "timeZone", "Europe/Budapest");
    }

    @Test
    void getWeatherByCoordinates_whenApiReturnsValidResponse_thenMapsResponse() {
        String jsonBody = """
            {
              "latitude": 47.49801,
              "longitude": 19.03991,
              "generationtime_ms": 0.123,
              "utc_offset_seconds": 3600,
              "timezone": "Europe/Budapest",
              "timezone_abbreviation": "CET",
              "elevation": 100,
              "hourly_units": {
                "temperature_2m": "°C",
                "relative_humidity_2m": "%",
                "snow_depth": "cm",
                "wind_speed_10m": "km/h",
                "cloud_cover": "%",
                "wind_speed_80m": "km/h"
              },
              "hourly": {
                "time": [
                  "2025-02-10T10:00",
                  "2025-02-10T11:00"
                ],
                "temperature_2m": [5.1, 5.4],
                "relative_humidity_2m": [80, 78],
                "snow_depth": [0.0, 0.0],
                "snowfall": [0.0, 0.0],
                "precipitation": [0.0, 0.0],
                "showers": [0.0, 0.0],
                "rain": [0.0, 0.0],
                "visibility": [10000, 10000],
                "wind_speed_10m": [10.5, 11.0],
                "cloud_cover": [50, 60],
                "wind_speed_80m": [15.0, 15.5]
              }
            }
            """;

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, jsonBody)));

        Mono<WeatherResponse> result =
                testedObject.getWeatherByCoordinates(47.49801, 19.03991, LocalDate.of(2025, 2, 10));

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.isPresent()).isTrue();
                    assertThat(response.getLatitude()).isEqualTo(47.49801);
                    assertThat(response.getLongitude()).isEqualTo(19.03991);
                    assertThat(response.getTimezone()).isEqualTo("Europe/Budapest");
                    assertThat(response.getHourly()).isNotNull();
                    assertThat(response.getHourly().getTime()).hasSize(2);
                    assertThat(response.getHourly().getTemperature2m()).containsExactly(5.1, 5.4);
                })
                .verifyComplete();
    }

    @Test
    void getWeatherByCoordinates_whenBodyIsInvalidJson_thenEmitsExternalApiFormatMismatchException() {
        String invalidJson = "this is not valid json";

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, invalidJson)));

        Mono<WeatherResponse> result =
                testedObject.getWeatherByCoordinates(47.49801, 19.03991, LocalDate.of(2025, 2, 10));

        StepVerifier.create(result)
                .expectError(ExternalApiFormatMismatchException.class)
                .verify();
    }

    @Test
    void getWeatherByCoordinates_whenApiReturnsNon2xxStatus_thenEmitsExternalApiException() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.INTERNAL_SERVER_ERROR, "error")));

        Mono<WeatherResponse> result =
                testedObject.getWeatherByCoordinates(47.49801, 19.03991, LocalDate.of(2025, 2, 10));

        StepVerifier.create(result)
                .expectError(ExternalApiException.class)
                .verify();
    }

    private ClientResponse buildClientResponse(HttpStatusCode status, String body) {
        HttpRequest request = new HttpRequest() {
            @Override
            public HttpMethod getMethod() {
                return HttpMethod.GET;
            }

            @Override
            public URI getURI() {
                return URI.create(BASE_URL + "/dummy");
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Map.of();
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };

        return ClientResponse
                .create(status)
                .request(request)
                .body(body)
                .build();
    }
}
