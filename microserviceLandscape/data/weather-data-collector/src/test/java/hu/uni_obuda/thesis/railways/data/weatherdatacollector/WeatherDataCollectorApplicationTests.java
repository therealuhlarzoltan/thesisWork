package hu.uni_obuda.thesis.railways.data.weatherdatacollector;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
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
import java.time.LocalDateTime;

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
public class WeatherDataCollectorApplicationTests {

	@Value("${retry.instances.getWeatherDataApi.maxAttempts:3}")
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
		registry.add("weather.api.base-url", () -> baseUrl);
	}

	@AfterAll
	public static void shutdownServer() throws Exception {
		mockWebServer.shutdown();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void getWeatherInfo_weatherApiSuccess_weatherInfoReturned() {
		String stationName = "Budapest-Nyugati";
		double latitude = 47.4979;
		double longitude = 19.0402;
		LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0);

		String validJson = """
				{
				  "latitude": 47.4979,
				  "longitude": 19.0402,
				  "generationtime_ms": 0.1,
				  "utc_offset_seconds": 0,
				  "timezone": "Europe/Budapest",
				  "timezone_abbreviation": "CET",
				  "elevation": 100,
				  "hourly": {
				    "time": [
				      "2024-01-01T00:00"
				    ],
				    "temperature_2m": [10.0],
				    "relative_humidity_2m": [75.0],
				    "snow_depth": [0.0],
				    "snowfall": [0.0],
				    "precipitation": [0.0],
				    "showers": [0.0],
				    "rain": [0.0],
				    "visibility": [9000],
				    "wind_speed_10m": [7.0],
				    "cloud_cover": [60],
				    "wind_speed_80m": [12.0]
				  }
				}
				""";

		mockWebServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", "application/json")
				.setBody(validJson));

		WeatherInfo response = webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/collect-weather-info")
						.queryParam("stationName", stationName)
						.queryParam("latitude", latitude)
						.queryParam("longitude", longitude)
						.queryParam("dateTime", dateTime.toString())
						.build())
				.exchange()
				.expectStatus().isOk()
				.expectBody(WeatherInfo.class)
				.returnResult()
				.getResponseBody();

		assertNotNull(response);
		assertEquals(stationName, response.getAddress());

		assertEquals(47.4979, response.getLatitude());
		assertEquals(19.0402, response.getLongitude());
		assertEquals(dateTime, response.getTime());


		assertEquals(10.0, response.getTemperature());
		assertEquals(75.0, response.getRelativeHumidity());
		assertEquals(7.0, response.getWindSpeedAt10m());
		assertEquals(12.0, response.getWindSpeedAt80m());
		assertEquals(60, response.getCloudCoverPercentage());
		assertEquals(9000, response.getVisibilityInMeters());

	}

	@Test
	void getWeatherInfo_weatherApiFailure_fallbackWeatherInfoReturned() {
		String stationName = "Budapest-Keleti";
		double latitude = 47.5;
		double longitude = 19.1;
		LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0);

		String invalidJson = "this is not valid json at all";

		for (int i = 0; i < retryAttempts + 1; i++) {
			mockWebServer.enqueue(new MockResponse()
					.setResponseCode(200)
					.setHeader("Content-Type", "application/json")
					.setBody(invalidJson));
		}

		WeatherInfo response = webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/collect-weather-info")
						.queryParam("stationName", stationName)
						.queryParam("latitude", latitude)
						.queryParam("longitude", longitude)
						.queryParam("dateTime", dateTime.toString())
						.build())
				.exchange()
				.expectStatus().isOk()
				.expectBody(WeatherInfo.class)
				.returnResult()
				.getResponseBody();

		assertNotNull(response);

		assertEquals(stationName, response.getAddress());
		assertEquals(latitude, response.getLatitude());
		assertEquals(longitude, response.getLongitude());
		assertEquals(dateTime, response.getTime());


		assertNull(response.getTemperature());
		assertNull(response.getRelativeHumidity());
		assertNull(response.getWindSpeedAt10m());
		assertNull(response.getWindSpeedAt80m());
		assertNull(response.getCloudCoverPercentage());
		assertNull(response.getVisibilityInMeters());
		assertNull(response.getSnowDepth());
		assertNull(response.getSnowFall());
		assertNull(response.getPrecipitation());
		assertNull(response.getRain());
		assertNull(response.getShowers());
		assertNull(response.getIsSnowing());
		assertNull(response.getIsRaining());
	}
}
