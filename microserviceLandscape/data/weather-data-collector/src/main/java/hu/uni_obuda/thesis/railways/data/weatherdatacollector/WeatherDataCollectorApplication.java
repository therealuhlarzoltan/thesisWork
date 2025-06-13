package hu.uni_obuda.thesis.railways.data.weatherdatacollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"hu.uni_obuda.thesis.railways"})
public class WeatherDataCollectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeatherDataCollectorApplication.class, args);
	}

}
