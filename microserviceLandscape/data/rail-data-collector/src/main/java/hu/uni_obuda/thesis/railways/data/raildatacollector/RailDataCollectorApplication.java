package hu.uni_obuda.thesis.railways.data.raildatacollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"hu.uni_obuda.thesis.railways"})
public class RailDataCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(RailDataCollectorApplication.class, args);
    }

}
