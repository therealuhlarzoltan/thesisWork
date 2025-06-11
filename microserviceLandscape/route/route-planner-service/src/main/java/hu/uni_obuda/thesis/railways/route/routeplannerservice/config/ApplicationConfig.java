package hu.uni_obuda.thesis.railways.route.routeplannerservice.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Configuration
public class ApplicationConfig {

    private final ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .defaultHeader("Content-Type", "application/json")
                .filter(loadBalancerExchangeFilterFunction)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // makes it ISO8601
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
