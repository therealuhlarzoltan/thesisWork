package hu.uni_obuda.thesis.railways.data.raildatacollector;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@ActiveProfiles({"test", "data-source-emma"})
@AutoConfigureWebTestClient
class RailDataCollectorRoutePlanningTest {

    @Autowired
    private WebTestClient webTestClient;

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private static final MockWebServer mockWebServer;

    static {
        try {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MockWebServer", e);
        }
    }

    private static final String EXISTING_TRAIN_NUMBER = "2410";
    private static final LocalDate TODAY = LocalDate.now();
    private static final long START_EPOCH = epochMillis(TODAY, LocalTime.of(0, 30));
    private static final long END_EPOCH = epochMillis(TODAY, LocalTime.of(1, 0));

    @DynamicPropertySource
    static void alterBaseUrl(DynamicPropertyRegistry registry) {
        String baseUrl = mockWebServer.url("/").toString();
        registry.add("railway.api.base-url", () -> baseUrl);
    }

    @AfterAll
    static void shutdownWebServer() throws IOException {
        mockWebServer.shutdown();
    }


    @Test
    void planRoute_forValidTimetable_returnsMappedRoute() {
        mockWebServer.enqueue(graphQlResponse(
                buildTimetableGraphQlResponse(START_EPOCH, END_EPOCH)
        ));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/plan-route")
                        .queryParam("from", "Budapest")
                        .queryParam("fromLatitude", 47.4979)
                        .queryParam("fromLongitude", 19.0402)
                        .queryParam("to", "Vác")
                        .queryParam("toLatitude", 47.7757)
                        .queryParam("toLongitude", 19.1361)
                        .queryParam("date", TODAY.toString())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBodyList(TrainRouteResponse.class)
                .value(routeResponses -> {
                    assertThat(routeResponses).hasSize(1);

                    TrainRouteResponse route = routeResponses.getFirst();
                    assertThat(route.getTrains()).isNotNull();
                    assertThat(route.getTrains()).hasSize(1);

                    TrainRouteResponse.Train train = route.getTrains().getFirst();
                    assertThat(train.getTrainNumber()).isEqualTo(EXISTING_TRAIN_NUMBER);
                    assertThat(train.getLineNumber()).isEqualTo("R70");
                    assertThat(train.getFromStation()).isEqualTo("Budapest");
                    assertThat(train.getToStation()).isEqualTo("Vác");
                    assertThat(train.getFromTimeScheduled()).contains("00:30");
                    assertThat(train.getToTimeScheduled()).contains("01:00");
                    assertThat(train.getFromTimeActual()).isNotNull();
                    assertThat(train.getToTimeActual()).isNotNull();
                });
    }

    private static long epochMillis(LocalDate date, LocalTime time) {
        return date.atTime(time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private static MockResponse graphQlResponse(String body) {
        return new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json");
    }

    private static String buildTimetableGraphQlResponse(long startEpoch, long endEpoch) {
        return """
           {
             "data": {
               "plan": {
                 "itineraries": [
                   {
                     "duration": %d,
                     "numberOfTransfers": 0,
                     "endTime": %d,
                     "startTime": %d,
                     "waitingTime": 0,
                     "walkTime": 0,
                     "legs": [
                       {
                         "agency": {
                           "name": "MAV-START",
                           "timezone": "Europe/Budapest"
                         },
                         "arrivalDelay": 300,
                         "departureDelay": 600,
                         "distance": 40000.0,
                         "duration": 3600.0,
                         "endTime": %d,
                         "from": {
                           "lat": 47.4979,
                           "lon": 19.0402,
                           "name": "Budapest",
                           "vertexType": "NORMAL",
                           "stop": {
                             "code": "Budapest",
                             "id": "STOP_1",
                             "timezone": "Europe/Budapest"
                           }
                         },
                         "to": {
                           "lat": 47.7757,
                           "lon": 19.1361,
                           "name": "Vác",
                           "vertexType": "NORMAL",
                           "stop": {
                             "code": "Vác",
                             "id": "STOP_2",
                             "timezone": "Europe/Budapest"
                           }
                         },
                         "headsign": "Vác",
                         "mode": "RAIL",
                         "route": {
                           "longName": "R70"
                         },
                         "startTime": %d,
                         "transitLeg": true,
                         "serviceDate": "%s",
                         "trip": {
                           "tripShortName": "%s",
                           "tripHeadsign": "Vác",
                           "isThroughCoach": false
                         }
                       }
                     ]
                   }
                 ]
               }
             }
           }
           """.formatted(
                endEpoch - startEpoch,   // duration
                endEpoch,
                startEpoch,
                endEpoch,
                startEpoch,
                TODAY.toString(),
                EXISTING_TRAIN_NUMBER
        );
    }
}
