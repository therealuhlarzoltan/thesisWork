package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ApplicationConfig {

    @Value("${railway.api.base-url}")
    private String railwayApiUrl;


    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.baseUrl(railwayApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
