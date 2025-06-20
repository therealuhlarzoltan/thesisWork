package hu.uni_obuda.thesis.railways.route.routeplannerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"hu.uni_obuda.thesis.railways.util", "hu.uni_obuda.thesis.railways.route.routeplannerservice"})
public class RoutePlannerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoutePlannerServiceApplication.class, args);
    }

}
