package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.config.ApplicationConfig;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ElviraRailDataWebClientTest {

    private static final String BASE_URL = "https://railway.example.com";

    @Mock
    private ExchangeFunction exchangeFunction;

    private ElviraRailDataWebClient testedObject;

    @BeforeEach
    public void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .exchangeFunction(exchangeFunction)
                .build();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        ObjectMapper objectMapper = applicationConfig.elviraObjectMapper();

        testedObject = new ElviraRailDataWebClientImpl(webClient, objectMapper);

        ReflectionTestUtils.setField(testedObject, "railwayBaseUrl", BASE_URL);
        ReflectionTestUtils.setField(testedObject, "timetableGetterUri", "/timetable");
        ReflectionTestUtils.setField(testedObject, "trainDetailsGetterUri", "/train-details");
    }

    @Test
    public void getShortTimetable_whenApiReturnsValidResponse_thenMapsAndTrimsDetails() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        String jsonBody = """
                {
                  "timetable": [
                    {
                      "starttime": "08:00",
                      "destinationtime": "10:00",
                      "details": [
                        {
                          "train_info": {
                            "url": "u1",
                            "get_url": "g1",
                            "code": "c1",
                            "vsz_code": "v1"
                          }
                        },
                        {
                          "train_info": {
                            "url": "u2",
                            "get_url": "g2",
                            "code": "c2",
                            "vsz_code": "v2"
                          }
                        }
                      ]
                    },
                    {
                      "starttime": "09:00",
                      "destinationtime": "11:00",
                      "details": [
                        {
                          "train_info": {
                            "url": "u3",
                            "get_url": "g3",
                            "code": "c3",
                            "vsz_code": "v3"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, jsonBody)));

        Mono<ElviraShortTimetableResponse> result =
                testedObject.getShortTimetable("BUD", "DEB", date);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getTimetable()).hasSize(2);

                    ElviraShortTimetableResponse.TimetableEntry first =
                            response.getTimetable().getFirst();
                    ElviraShortTimetableResponse.TimetableEntry second =
                            response.getTimetable().get(1);

                    assertThat(first.getDetails()).hasSize(1);
                    assertThat(first.getDetails().getFirst().getTrainInfo().getUrl())
                            .isEqualTo("u1");

                    assertThat(second.getDetails()).hasSize(1);
                    assertThat(second.getDetails().getFirst().getTrainInfo().getCode())
                            .isEqualTo("c3");
                })
                .verifyComplete();
    }

    @Test
    public void getShortTimetable_whenBodyIsInvalidJson_thenEmitsExternalApiFormatMismatchException() {
        String invalidJson = "this is not valid json";

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, invalidJson)));

        Mono<ElviraShortTimetableResponse> result =
                testedObject.getShortTimetable("BUD", "DEB", LocalDate.of(2024, 10, 10));

        StepVerifier.create(result)
                .expectError(ExternalApiFormatMismatchException.class)
                .verify();
    }

    @Test
    public void getShortTimetable_whenApiReturnsNon2xxStatus_thenEmitsExternalApiException() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.INTERNAL_SERVER_ERROR, "error")));

        Mono<ElviraShortTimetableResponse> result =
                testedObject.getShortTimetable("BUD", "DEB", LocalDate.of(2024, 10, 10));

        StepVerifier.create(result)
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    public void getShortTrainDetails_whenApiReturnsValidResponse_thenMapsResponse() {
        String jsonBody = """
                {
                  "stations": [
                    {
                      "station": {
                        "url": "/station/1",
                        "get_url": "/station/1/details",
                        "code": "ST1"
                      },
                      "schedule": {
                        "departure": "08:00",
                        "arrival": "07:00"
                      },
                      "real": {
                        "departure": "08:05",
                        "arrival": "07:05"
                      },
                      "expected": {
                        "departure": "08:03",
                        "arrival": "07:03"
                      }
                    }
                  ]
                }
                """;

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, jsonBody)));

        String thirdPartyUrl = "https://third-party.example.com/train/123";

        Mono<ElviraShortTrainDetailsResponse> result =
                testedObject.getShortTrainDetails(thirdPartyUrl);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStations()).hasSize(1);

                    ElviraShortTrainDetailsResponse.Station station =
                            response.getStations().getFirst();

                    assertThat(station.getCode()).isEqualTo("ST1");
                    assertThat(station.getUrl()).isEqualTo("/station/1");
                    assertThat(station.getGetUrl()).isEqualTo("/station/1/details");

                    assertThat(station.getScheduledDeparture()).isEqualTo("08:00");
                    assertThat(station.getScheduledArrival()).isEqualTo("07:00");
                    assertThat(station.getRealDeparture()).isEqualTo("08:05");
                    assertThat(station.getRealArrival()).isEqualTo("07:05");
                    assertThat(station.getExpectedDeparture()).isEqualTo("08:03");
                    assertThat(station.getExpectedArrival()).isEqualTo("07:03");
                })
                .verifyComplete();
    }

    @Test
    public void getShortTrainDetails_whenBodyIsInvalidJson_thenEmitsExternalApiFormatMismatchException() {
        String invalidJson = "{ not-json";

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, invalidJson)));

        Mono<ElviraShortTrainDetailsResponse> result =
                testedObject.getShortTrainDetails("https://third-party.example.com/train/123");

        StepVerifier.create(result)
                .expectError(ExternalApiFormatMismatchException.class)
                .verify();
    }

    @Test
    public void getShortTrainDetails_whenApiReturnsNon2xxStatus_thenEmitsExternalApiException() {
        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.BAD_GATEWAY, "bad gateway")));

        Mono<ElviraShortTrainDetailsResponse> result =
                testedObject.getShortTrainDetails("https://third-party.example.com/train/123");

        StepVerifier.create(result)
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    public void getTimetable_whenApiReturnsValidResponse_thenMapsTrainSegmentsAndTransferStations() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        String jsonBody = """
                {
                  "timetable": [
                    {
                      "details": [
                        {
                          "train_info": {
                            "url": "/train/1",
                            "get_url": "/train/1/details",
                            "code": "TR1",
                            "vsz_code": "V1"
                          }
                        },
                        {
                          "from": "Transfer Station",
                          "dep": "10:00",
                          "dep_real": "10:05"
                        }
                      ]
                    }
                  ]
                }
                """;

        when(exchangeFunction.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(buildClientResponse(HttpStatus.OK, jsonBody)));

        Mono<ElviraTimetableResponse> result =
                testedObject.getTimetable("BUD", "DEB", date);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getTimetable()).hasSize(1);

                    ElviraTimetableResponse.TimetableEntry entry =
                            response.getTimetable().getFirst();

                    List<ElviraTimetableResponse.TrainInfo> trainSegments =
                            entry.getTrainSegments();
                    List<ElviraTimetableResponse.TransferStation> transferStations =
                            entry.getTransferStations();

                    assertThat(trainSegments).hasSize(1);
                    assertThat(trainSegments.getFirst().getCode()).isEqualTo("TR1");
                    assertThat(trainSegments.getFirst().getUrl()).isEqualTo("/train/1");

                    assertThat(transferStations).hasSize(1);
                    ElviraTimetableResponse.TransferStation transfer = transferStations.getFirst();
                    assertThat(transfer.getStationName()).isEqualTo("Transfer Station");
                    assertThat(transfer.getScheduledArrival()).isEqualTo("10:00");
                    assertThat(transfer.getRealArrival()).isEqualTo("10:05");
                })
                .verifyComplete();
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
