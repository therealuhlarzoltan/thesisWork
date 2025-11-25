package hu.uni_obuda.thesis.railways.data.delaydatacollector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.flyway.enabled=false"
        }
)
@ActiveProfiles({"test", "production", "data-source-elvira"})
class DelayDataCollectorApplicationTests {

    @Test
    void contextLoads() {
    }

}
