package hu.uni_obuda.thesis.railways.data.geocodingservice.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.response.CoordinatesResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapsWebClientTest {

    private static final String BASE_URL = "https://maps.example.com";

    @Mock
    private ExchangeFunction exchangeFunction;

    private MapsWebClient testedObject;

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .exchangeFunction(exchangeFunction)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        testedObject = new MapsWebClientImpl(webClient, objectMapper);

        ReflectionTestUtils.setField(testedObject, "mapsBaseUrl", BASE_URL);
        ReflectionTestUtils.setField(testedObject, "geocodingUri", "/geocode/json");
        ReflectionTestUtils.setField(testedObject, "countryCode", "HU");
        ReflectionTestUtils.setField(testedObject, "apiKey", "dummy-api-key");
    }

    @Test
    void getCoordinates_whenApiReturnsValidResponse_thenMapsCoordinates() {
        String jsonBody = """
                {
                  "results": [
                    {
                      "geometry": {
                        "location": {
                          "lat": 47.49801,
                          "lng": 19.03991
                        }
                      }
                    }
                  ]
                }
                """;

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, jsonBody)));

        Mono<CoordinatesResponse> result =
                testedObject.getCoordinates("Budapest, Kőbánya-Kispest");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.isPresent()).isTrue();
                    assertThat(response.getLatitude()).isEqualTo(47.49801);
                    assertThat(response.getLongitude()).isEqualTo(19.03991);
                })
                .verifyComplete();
    }

    @Test
    void getCoordinates_whenBodyIsInvalidJson_thenEmitsExternalApiFormatMismatchException() {
        String invalidJson = "this is not valid json";

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, invalidJson)));

        Mono<CoordinatesResponse> result =
                testedObject.getCoordinates("Budapest, Kőbánya-Kispest");

        StepVerifier.create(result)
                .expectError(ExternalApiFormatMismatchException.class)
                .verify();
    }

    @Test
    void getCoordinates_whenApiReturnsNon2xxStatus_thenEmitsExternalApiException() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.INTERNAL_SERVER_ERROR, "error")));

        Mono<CoordinatesResponse> result =
                testedObject.getCoordinates("Budapest, Kőbánya-Kispest");

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