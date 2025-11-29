package hu.uni_obuda.thesis.railways.data.geocodingservice;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false"
        }
)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
public class GeocodingServiceApplicationTests {

    @Value("${retry.instances.geocodingApi.maxAttempts:3}")
    private int retryAttempts;

    @Autowired
    private WebTestClient webTestClient;

    private static final MockWebServer mockWebServer;


    static {
        try {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MockWebServer", e);
        }
    }

    @DynamicPropertySource
    public static void alterBaseUrl(DynamicPropertyRegistry registry) {
        String baseUrl = mockWebServer.url("/").toString().replaceAll("/$", "");
        registry.add("maps.api.base-url", () -> baseUrl);
    }

    @AfterAll
    public static void shutdownServer() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void contextLoads() {
    }

    @Test
    void getCoordinates_geocodingSuccess_coordinatesReturned() {
        String address = "Budapest-Nyugati";
        String validJson = """
                {
                  "results": [
                    {
                      "geometry": {
                        "location": {
                          "lat": 47.4979,
                          "lng": 19.0402
                        }
                      }
                    }
                  ]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(validJson));

        GeocodingResponse response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coordinates")
                        .queryParam("address", address)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(GeocodingResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(47.4979, response.getLatitude());
        assertEquals(19.0402, response.getLongitude());
        assertEquals(address, response.getAddress());
        assertFalse(response.isEmpty(), "Response should NOT be empty for valid external JSON");
    }

    @Test
    void getCoordinates_geocodingFailure_fallbackResponseReturned() {
        String address = "Budapest-Északi";
        String invalidJson = """
                [
                  {
                    "geometry": {
                      "location": {
                        "lat": 47.4979,
                        "long": 19.0402
                      }
                    }
                  }
                ]
                """;

        for (int i = 0; i < retryAttempts + 1; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(invalidJson));
        }

        GeocodingResponse response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coordinates")
                        .queryParam("address", address)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(GeocodingResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getLatitude());
        assertNull(response.getLongitude());
        assertEquals(address, response.getAddress());
        assertTrue(response.isEmpty(), "Fallback response should be empty (null coords)");
    }
}
