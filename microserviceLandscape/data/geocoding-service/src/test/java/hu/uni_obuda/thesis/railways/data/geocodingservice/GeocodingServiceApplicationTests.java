package hu.uni_obuda.thesis.railways.data.geocodingservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

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
public class GeocodingServiceApplicationTests {

    @Autowired
    private WebTestClient testWebClient;

    @MockitoBean
    private WebClient mockWebClient;

    @Test
    public void contextLoads() {
    }



}
