package hu.uni_obuda.thesis.railways.data.delaydatacollector;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
        }
)
@ActiveProfiles({"test", "local-debug", "data-source-emma"})
@AutoConfigureWebTestClient
class DelayDataCollectorTrainRouteTest {

    @ServiceConnection("postgres")
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @ServiceConnection("redis")
    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @Autowired
    WebTestClient webTestClient;

    private static final String BASE_PATH = "/train-routes";
    private static final String NON_EXISTENT_TRAIN_NUMBER = "NON_EXIST_0001";

    @Test
    void createTrainRoute_validRequest_returnsPersistedRoute() {
        String trainNumber = generateTrainNumber();
        String lineNumber = "LN-" + trainNumber;
        String startStation = "Budapest";
        String endStation = "Vienna";

        TrainRouteRequest request = new TrainRouteRequest(
                trainNumber,
                lineNumber,
                startStation,
                endStation
        );

        TrainRouteResponse response = webTestClient.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(TrainRouteResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getTrainNumber()).isEqualTo(trainNumber);
        assertThat(response.getLineNumber()).isEqualTo(lineNumber);
        assertThat(response.getStartStation()).isEqualTo(startStation);
        assertThat(response.getEndStation()).isEqualTo(endStation);
    }

    @Test
    void createTrainRoute_blankTrainNumber_returnsBadRequest() {
        TrainRouteRequest request = new TrainRouteRequest(
                "",
                "LN-1",
                "Budapest",
                "Vienna"
        );

        webTestClient.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createTrainRoute_blankLineNumber_returnsBadRequest() {
        TrainRouteRequest request = new TrainRouteRequest(
                generateTrainNumber(),
                "",
                "Budapest",
                "Vienna"
        );

        webTestClient.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createTrainRoute_blankStartStation_returnsBadRequest() {
        TrainRouteRequest request = new TrainRouteRequest(
                generateTrainNumber(),
                "LN-1",
                "",
                "Vienna"
        );

        webTestClient.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createTrainRoute_blankEndStation_returnsBadRequest() {
        TrainRouteRequest request = new TrainRouteRequest(
                generateTrainNumber(),
                "LN-1",
                "Budapest",
                ""
        );

        webTestClient.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getAllTrainRoutes_multipleRoutesCreated_returnsListContainingCreatedRoutes() {
        TrainRouteResponse route1 = createTrainRoute(generateTrainNumber());
        TrainRouteResponse route2 = createTrainRoute(generateTrainNumber());

        List<TrainRouteResponse> routes = webTestClient.get()
                .uri(BASE_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TrainRouteResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(routes).isNotNull();
        assertThat(routes)
                .extracting(TrainRouteResponse::getTrainNumber)
                .contains(route1.getTrainNumber(), route2.getTrainNumber());
    }

    @Test
    void getTrainRoute_existingTrainNumber_returnsRouteWithExpectedFields() {
        TrainRouteResponse created = createTrainRoute(generateTrainNumber());

        List<TrainRouteResponse> fetchedList = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH)
                        .queryParam("trainNumber", created.getTrainNumber())
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TrainRouteResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetchedList).isNotNull();
        assertThat(fetchedList).hasSize(1);

        TrainRouteResponse fetched = fetchedList.getFirst();
        assertThat(fetched.getTrainNumber()).isEqualTo(created.getTrainNumber());
        assertThat(fetched.getLineNumber()).isEqualTo(created.getLineNumber());
        assertThat(fetched.getStartStation()).isEqualTo(created.getStartStation());
        assertThat(fetched.getEndStation()).isEqualTo(created.getEndStation());
    }

    @Test
    void getTrainRoute_nonExistingTrainNumber_returnsNotFound() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH)
                        .queryParam("trainNumber", NON_EXISTENT_TRAIN_NUMBER)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateTrainRoute_existingTrainNumber_updatesRoute() {
        TrainRouteResponse created = createTrainRoute(generateTrainNumber());

        TrainRouteRequest updateRequest = new TrainRouteRequest(
                created.getTrainNumber(),
                "UPDATED-LINE",
                "Updated Start",
                "Updated End"
        );

        TrainRouteResponse updated = webTestClient.put()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(TrainRouteResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.getTrainNumber()).isEqualTo(created.getTrainNumber());
        assertThat(updated.getStartStation()).isEqualTo("Updated Start");
        assertThat(updated.getEndStation()).isEqualTo("Updated End");
    }

    @Test
    void updateTrainRoute_nonExistingTrainNumber_returnsNotFound() {
        TrainRouteRequest updateRequest = new TrainRouteRequest(
                NON_EXISTENT_TRAIN_NUMBER,
                "LN-404",
                "Nowhere",
                "Nowhere Else"
        );

        webTestClient.put()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteTrainRoute_existingTrainNumber_deletesRouteAndSubsequentGetReturnsNotFound() {
        TrainRouteResponse created = createTrainRoute(generateTrainNumber());

        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH)
                        .queryParam("trainNumber", created.getTrainNumber())
                        .build())
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH)
                        .queryParam("trainNumber", created.getTrainNumber())
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteTrainRoute_nonExistingTrainNumber_returnsNotFound() {
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH)
                        .queryParam("trainNumber", NON_EXISTENT_TRAIN_NUMBER)
                        .build())
                .exchange()
                .expectStatus().isNotFound();
    }

    private TrainRouteResponse createTrainRoute(String trainNumber) {
        TrainRouteRequest request = new TrainRouteRequest(
                trainNumber,
                "LN-" + trainNumber,
                "Start-" + trainNumber,
                "End-" + trainNumber
        );

        TrainRouteResponse response = webTestClient.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(TrainRouteResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        return response;
    }

    private String generateTrainNumber() {
        String nano = Long.toString(System.nanoTime());
        if (nano.length() > 14) {
            nano = nano.substring(nano.length() - 14);
        }
        return "TR" + nano;
    }
}
