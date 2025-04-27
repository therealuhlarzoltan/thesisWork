package hu.uni_obuda.thesis.railways.data.delaydatacollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class DelayDataCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DelayDataCollectorApplication.class, args);
    }

}
