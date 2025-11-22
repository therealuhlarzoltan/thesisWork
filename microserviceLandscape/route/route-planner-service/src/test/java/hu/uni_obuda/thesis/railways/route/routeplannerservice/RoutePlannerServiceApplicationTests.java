package hu.uni_obuda.thesis.railways.route.routeplannerservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
    }
)
@ActiveProfiles("test")
class RoutePlannerServiceApplicationTests {

}
