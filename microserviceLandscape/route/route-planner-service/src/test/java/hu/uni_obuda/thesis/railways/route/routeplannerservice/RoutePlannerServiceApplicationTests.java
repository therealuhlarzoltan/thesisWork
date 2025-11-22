package hu.uni_obuda.thesis.railways.route.routeplannerservice;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.helper.TimetableProcessingHelper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@ActiveProfiles({"test", "data-source-emma"})
@AutoConfigureWebTestClient(timeout = "PT30S")
class RoutePlannerServiceApplicationTests {

    @TestConfiguration
    static class TestApplicationConfig {

        @Primary
        @Bean("webClientWithoutRedirects")
        public WebClient motLoadBalancedWebClientWithoutRedirects(WebClient.Builder builder) {
            return builder
                    .defaultHeader("Accept", "application/json")
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TimetableProcessingHelper timetableProcessingHelper;

    private static final MockWebServer GEOCODING_SERVER;
    private static final MockWebServer RAIL_SERVER;
    private static final MockWebServer STATION_SERVER;
    private static final MockWebServer WEATHER_SERVER;
    private static final MockWebServer PREDICTOR_SERVER;

    static {
        try {
            GEOCODING_SERVER = new MockWebServer();
            RAIL_SERVER = new MockWebServer();
            STATION_SERVER = new MockWebServer();
            WEATHER_SERVER = new MockWebServer();
            PREDICTOR_SERVER = new MockWebServer();

            GEOCODING_SERVER.start();
            RAIL_SERVER.start();
            STATION_SERVER.start();
            WEATHER_SERVER.start();
            PREDICTOR_SERVER.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MockWebServers", e);
        }
    }

    @DynamicPropertySource
    static void alterBaseUrls(DynamicPropertyRegistry registry) {
        String geocodingBase = GEOCODING_SERVER.url("/").toString().replaceAll("/$", "");
        String emmaRailBase = RAIL_SERVER.url("/").toString().replaceAll("/$", "");
        String stationBase = STATION_SERVER.url("/").toString().replaceAll("/$", "");
        String weatherBase = WEATHER_SERVER.url("/").toString().replaceAll("/$", "");
        String predictorBase = PREDICTOR_SERVER.url("/").toString().replaceAll("/$", "");

        registry.add("app.geocoding-service-url", () -> geocodingBase);
        registry.add("app.rail-data-collector-url", () -> emmaRailBase);
        registry.add("app.delay-data-collector-url", () -> stationBase);
        registry.add("app.weather-data-collector-url", () -> weatherBase);
        registry.add("app.delay-predictor-service-url", () -> predictorBase);
    }

    @AfterAll
    static void shutdownServers() throws IOException {
        GEOCODING_SERVER.shutdown();
        RAIL_SERVER.shutdown();
        STATION_SERVER.shutdown();
        WEATHER_SERVER.shutdown();
        PREDICTOR_SERVER.shutdown();
    }

    @BeforeEach
    void setup() {
        Mockito.reset(timetableProcessingHelper);

        when(timetableProcessingHelper.filterByDeparture(any(LocalDateTime.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(timetableProcessingHelper.filterByArrival(any(LocalDateTime.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(timetableProcessingHelper.filterByChanges(anyInt(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void contextLoads() {

    }

    @Test
    void planRoute_fromBlank_returns4xx() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", "")
                        .queryParam("to", "ToStation")
                        .queryParam("departureTime", LocalDateTime.now().plusDays(1).toString())
                        .build())
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void planRoute_toBlank_returns4xx() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", "FromStation")
                        .queryParam("to", "")
                        .queryParam("departureTime", LocalDateTime.now().plusDays(1).toString())
                        .build())
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void planRoute_bothDatesMissing_returns4xx() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", "FromStation")
                        .queryParam("to", "ToStation")
                        .build())
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void planRoute_arrivalBeforeNow_returns4xx() {
        String arrivalPast = "2000-01-01T10:00:00";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", "FromStation")
                        .queryParam("to", "ToStation")
                        .queryParam("arrivalTime", arrivalPast)
                        .build())
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void planRoute_arrivalBeforeDeparture_returns4xx() {
        String departure = "2999-01-02T10:00:00";
        String arrival   = "2999-01-01T10:00:00";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", "FromStation")
                        .queryParam("to", "ToStation")
                        .queryParam("departureTime", departure)
                        .queryParam("arrivalTime", arrival)
                        .build())
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void planRoute_negativeMaxChanges_returns4xx() {
        String departure = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).toString();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", "FromStation")
                        .queryParam("to", "ToStation")
                        .queryParam("departureTime", departure)
                        .queryParam("maxChanges", -1)
                        .build())
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void planRoute_trainAlreadyEnRoute_usesActualTimes() {
        String from = "Budapest-Nyugati";
        String to = "Szolnok";
        String departureStr = "2025-01-01T08:00:00";

        GEOCODING_SERVER.enqueue(jsonResponse(200, geocodingJson(from, 47.4979, 19.0402)));
        GEOCODING_SERVER.enqueue(jsonResponse(200, geocodingJson(to, 47.1730, 20.1950)));

        String timetableJson = """
            [
              {
                "trains": [
                  {
                    "trainNumber": "IC 123",
                    "lineNumber": "70",
                    "fromStation": "Budapest-Nyugati",
                    "toStation": "Szolnok",
                    "fromTimeScheduled": "2025-01-01T10:00",
                    "toTimeScheduled": "2025-01-01T11:00",
                    "fromTimeActual": "2025-01-01T10:02",
                    "toTimeActual": "2025-01-01T11:05"
                  }
                ]
              }
            ]
            """;
        RAIL_SERVER.enqueue(jsonResponse(200, timetableJson));

        List<RouteResponse> routes = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("departureTime", departureStr)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RouteResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(routes);
        assertEquals(1, routes.size());
        RouteResponse route = routes.getFirst();
        assertNotNull(route.getTrains());
        assertEquals(1, route.getTrains().size());

        RouteResponse.Train train = route.getTrains().getFirst();
        assertEquals("IC 123", train.getTrainNumber());
        assertEquals("2025-01-01T10:02", train.getFromTimeActual());
        assertEquals("2025-01-01T11:05", train.getToTimeActual());
        assertNull(train.getFromTimePredicted());
        assertNull(train.getToTimePredicted());
    }

    @Test
    void planRoute_trainNotEnRoute_predictionsUsed() {
        String from = "Budapest-Nyugati";
        String to = "Szolnok";
        String departureStr = "2025-01-01T08:00:00";

        GEOCODING_SERVER.enqueue(jsonResponse(200, geocodingJson(from, 47.4979, 19.0402)));
        GEOCODING_SERVER.enqueue(jsonResponse(200, geocodingJson(to, 47.1730, 20.1950)));

        String timetableJsonNoActuals = """
            [
              {
                "trains": [
                  {
                    "trainNumber": "IC 123",
                    "lineNumber": "70",
                    "fromStation": "Budapest-Nyugati",
                    "toStation": "Szolnok",
                    "fromTimeScheduled": "2025-01-01T10:00",
                    "toTimeScheduled": "2025-01-01T11:00"
                  }
                ]
              }
            ]
            """;
        RAIL_SERVER.enqueue(jsonResponse(200, timetableJsonNoActuals));

        String trainRouteJson = """
            [
              {
                "trains": [
                  {
                    "trainNumber": "IC 123",
                    "lineNumber": "70",
                    "fromStation": "FROM_STATION",
                    "toStation": "TO_STATION",
                    "fromTimeScheduled": "2025-01-01T10:00",
                    "toTimeScheduled": "2025-01-01T11:00"
                  }
                ]
              }
            ]
            """;
        STATION_SERVER.enqueue(jsonResponse(200, trainRouteJson));

        String stationJson = """
            [
              {
                "stationCode": "FROM_STATION",
                "latitude": 47.5000,
                "longitude": 19.0500
              }
            ]
            """;
        String stationJson2 = """
            [
              {
                "stationCode": "TO_STATION",
                "latitude": 47.2000,
                "longitude": 20.2000
              }
            ]
            """;
        STATION_SERVER.enqueue(jsonResponse(200, stationJson));
        STATION_SERVER.enqueue(jsonResponse(200, stationJson2));

        String weatherJson = """
            {
              "time": "2025-01-01T10:00",
              "address": "FROM_STATION",
              "latitude": 47.5000,
              "longitude": 19.0500,
              "temperature": 20.0
            }
            """;
        String weatherJson2 = """
            {
              "time": "2025-01-01T11:00",
              "address": "TO_STATION",
              "latitude": 47.2000,
              "longitude": 20.2000,
              "temperature": 18.0
            }
            """;
        WEATHER_SERVER.enqueue(jsonResponse(200, weatherJson));
        WEATHER_SERVER.enqueue(jsonResponse(200, weatherJson2));

        String predictionJson = """
            {
              "trainNumber": "123",
              "stationCode": "FROM_STATION",
              "predictedDelay": 5.0
            }
            """;
        String predictionJson2 = """
            {
              "trainNumber": "123",
              "stationCode": "TO_STATION",
              "predictedDelay": 5.0
            }
            """;
        PREDICTOR_SERVER.enqueue(jsonResponse(200, predictionJson));
        PREDICTOR_SERVER.enqueue(jsonResponse(200, predictionJson2));

        List<RouteResponse> routes = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("departureTime", departureStr)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RouteResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(routes);
        assertEquals(1, routes.size());
        RouteResponse route = routes.getFirst();
        assertEquals(1, route.getTrains().size());

        RouteResponse.Train train = route.getTrains().getFirst();
        assertEquals("IC 123", train.getTrainNumber());
        assertEquals("2025-01-01T10:00", train.getFromTimeScheduled());
        assertEquals("2025-01-01T11:00", train.getToTimeScheduled());

        assertEquals("2025-01-01T10:05", train.getFromTimePredicted());
        assertEquals("2025-01-01T11:05", train.getToTimePredicted());

        assertNull(train.getFromTimeActual());
        assertNull(train.getToTimeActual());
    }

    @Test
    void planRoute_stationCoordinatesMissing_fallsBackToScheduledTimes() {
        String from = "Budapest-Nyugati";
        String to = "Szolnok";
        String departureStr = "2025-01-01T08:00:00";

        GEOCODING_SERVER.enqueue(jsonResponse(200, geocodingJson(from, 47.4979, 19.0402)));
        GEOCODING_SERVER.enqueue(jsonResponse(200, geocodingJson(to, 47.1730, 20.1950)));

        String timetableJsonNoActuals = """
            [
              {
                "trains": [
                  {
                    "trainNumber": "IC 123",
                    "lineNumber": "70",
                    "fromStation": "Budapest-Nyugati",
                    "toStation": "Szolnok",
                    "fromTimeScheduled": "2025-01-01T10:00",
                    "toTimeScheduled": "2025-01-01T11:00"
                  }
                ]
              }
            ]
            """;
        RAIL_SERVER.enqueue(jsonResponse(200, timetableJsonNoActuals));

        String trainRouteJson = """
            [
              {
                "trains": [
                  {
                    "trainNumber": "IC 123",
                    "lineNumber": "70",
                    "fromStation": "FROM_STATION",
                    "toStation": "TO_STATION",
                    "fromTimeScheduled": "2025-01-01T10:00",
                    "toTimeScheduled": "2025-01-01T11:00"
                  }
                ]
              }
            ]
            """;
        STATION_SERVER.enqueue(jsonResponse(200, trainRouteJson));

        String stationNullCoordsJson = """
            [
              {
                "stationCode": "FROM_STATION",
                "latitude": null,
                "longitude": null
              }
            ]
            """;
        String stationNullCoordsJson2 = """
            [
              {
                "stationCode": "TO_STATION",
                "latitude": null,
                "longitude": null
              }
            ]
            """;
        STATION_SERVER.enqueue(jsonResponse(200, stationNullCoordsJson));
        STATION_SERVER.enqueue(jsonResponse(200, stationNullCoordsJson2));

        List<RouteResponse> routes = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/plan")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("departureTime", departureStr)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RouteResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(routes);
        assertEquals(1, routes.size());
        RouteResponse route = routes.getFirst();
        assertEquals(1, route.getTrains().size());

        RouteResponse.Train train = route.getTrains().getFirst();
        assertEquals("2025-01-01T10:00", train.getFromTimeScheduled());
        assertEquals("2025-01-01T11:00", train.getToTimeScheduled());
        assertNull(train.getFromTimeActual());
        assertNull(train.getToTimeActual());
        assertNull(train.getFromTimePredicted());
        assertNull(train.getToTimePredicted());
    }

    private static MockResponse jsonResponse(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String geocodingJson(String address, double lat, double lon) {
        return String.format(Locale.ROOT,
                """
                {
                  "latitude": %.6f,
                  "longitude": %.6f,
                  "address": "%s"
                }
                """,
                lat, lon, address
        );
    }
}
