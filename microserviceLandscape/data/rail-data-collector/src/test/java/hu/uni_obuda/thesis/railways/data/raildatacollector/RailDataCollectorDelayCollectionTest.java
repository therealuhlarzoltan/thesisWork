package hu.uni_obuda.thesis.railways.data.raildatacollector;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
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

import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@ActiveProfiles({"test", "data-source-emma"})
@AutoConfigureWebTestClient
class RailDataCollectorDelayCollectionTest {

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
    private static final String NON_EXISTING_TRAIN_NUMBER = "9999";
    private static final String GTFS_ID = "1:26892408";
    private static final LocalDate TODAY = LocalDate.now();
    private static final long START_EPOCH = epochMillis(TODAY, LocalTime.of(0, 30));
    private static final long END_EPOCH = epochMillis(TODAY, LocalTime.of(1, 0));

    @DynamicPropertySource
    static void alterBaseUrl(DynamicPropertyRegistry registry) {
        String baseUrl = mockWebServer.url("/").toString();
        registry.add("railway.api.base-url", () -> baseUrl);
    }

    @BeforeAll
    static void setUpTimetableResponse() {
        mockWebServer.enqueue(graphQlResponse(
                buildShortTimetableGraphQlResponse(START_EPOCH, END_EPOCH, EXISTING_TRAIN_NUMBER, GTFS_ID)
        ));
    }

    @AfterAll
    static void shutdownWebServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getDelayInfo_forExistingTrain_returnsDelayInfo() {
        mockWebServer.enqueue(graphQlResponse(
                buildShortTrainDetailsGraphQlResponse(EXISTING_TRAIN_NUMBER)
        ));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/collect")
                        .queryParam("trainId", EXISTING_TRAIN_NUMBER)
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
                .expectBodyList(DelayInfo.class)
                .value(delays -> {
                    assertThat(delays).hasSize(3);

                    assertThat(delays)
                            .extracting(DelayInfo::getStationCode, DelayInfo::getArrivalDelay)
                            .containsExactly(
                                    tuple("Budapest", null),
                                    tuple("Dunakeszi", 5),
                                    tuple("Vác", -1)
                            );

                    assertThat(delays)
                            .extracting(DelayInfo::getStationCode, DelayInfo::getDepartureDelay)
                            .containsExactly(
                                    tuple("Budapest", 10),
                                    tuple("Dunakeszi", 5),
                                    tuple("Vác", null)
                            );
                });
    }

    @Test
    void getDelayInfo_forTrainNotInService_returnsTooEarly() {

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/collect")
                        .queryParam("trainId", NON_EXISTING_TRAIN_NUMBER)
                        .queryParam("from", "Budapest")
                        .queryParam("fromLatitude", 47.4979)
                        .queryParam("fromLongitude", 19.0402)
                        .queryParam("to", "Vác")
                        .queryParam("toLatitude", 47.7757)
                        .queryParam("toLongitude", 19.1361)
                        .queryParam("date", TODAY.toString())
                        .build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_EARLY);
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

    private static String buildShortTimetableGraphQlResponse(long startEpoch,
                                                             long endEpoch,
                                                             String trainNumber,
                                                             String gtfsId) {
        return """
               {
                 "data": {
                     "plan": {
                       "itineraries": [
                         {
                           "numberOfTransfers": 0,
                           "legs": [
                             {
                               "startTime": %d,
                               "endTime": %d,
                               "mode": "RAIL",
                               "route": {
                                 "longName": "R70"
                               },
                               "trip": {
                                 "tripShortName": "%s személyvonat",
                                 "tripHeadsign": "Vác",
                                 "gtfsId": "%s",
                                 "id": "opaque-id"
                               }
                             }
                           ]
                         }
                       ]
                     }
                   }
               }
               """.formatted(startEpoch, endEpoch, trainNumber, gtfsId);
    }


    private static String buildShortTrainDetailsGraphQlResponse(String trainNumber) {
        return """
               {
                 "data": {
                     "trip": {
                       "id": "opaque-id",
                       "route": {
                         "id": "route-1",
                         "mode": "RAIL",
                         "longName": "R70",
                         "type": 2
                       },
                       "tripShortName": "%s személyvonat",
                       "tripHeadsign": "Vác",
                       "serviceId": "SERVICE_1",
                       "stoptimes": [
                         {
                           "scheduledArrival": 36000,
                           "realtimeArrival": 36600,
                           "arrivalDelay": 600,
                           "scheduledDeparture": 36000,
                           "realtimeDeparture": 36600,
                           "departureDelay": 600,
                           "serviceDay": 1718400000,
                           "stop": {
                             "id": "STOP_1",
                             "stopId": "STOP_1",
                             "name": "Budapest",
                             "lat": 47.0,
                             "lon": 19.0
                           }
                         },
                         {
                           "scheduledArrival": 37200,
                           "realtimeArrival": 37500,
                           "arrivalDelay": 300,
                           "scheduledDeparture": 37200,
                           "realtimeDeparture": 37500,
                           "departureDelay": 300,
                           "serviceDay": 1718400000,
                           "stop": {
                             "id": "STOP_2",
                             "stopId": "STOP_2",
                             "name": "Dunakeszi",
                             "lat": 47.7,
                             "lon": 19.1
                           }
                         },
                         {
                           "scheduledArrival": 38400,
                           "realtimeArrival": 38400,
                           "arrivalDelay": -60,
                           "scheduledDeparture": 38400,
                           "realtimeDeparture": 38400,
                           "departureDelay": 0,
                           "serviceDay": 1718400000,
                           "stop": {
                             "id": "STOP_3",
                             "stopId": "STOP_3",
                             "name": "Vác",
                             "lat": 47.78,
                             "lon": 19.13
                           }
                         }
                       ],
                       "vehiclePositions": []
                     }
                   }
               }
               """.formatted(trainNumber);
    }
}
