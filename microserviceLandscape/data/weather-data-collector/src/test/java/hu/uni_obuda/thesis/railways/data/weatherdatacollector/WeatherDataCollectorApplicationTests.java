package hu.uni_obuda.thesis.railways.data.weatherdatacollector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
class WeatherDataCollectorApplicationTests {

	@Test
	void contextLoads() {
	}

}
