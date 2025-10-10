package hu.uni_obuda.thesis.railways.route.routeplannerservice.config;

import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class HealthCheckConfig {

    private final HealthCheckService healthCheckService;

    @Bean
    public ReactiveHealthContributor routePlannerDataCollectorServices() {
        Map<String, ReactiveHealthIndicator> statuses = new LinkedHashMap<>();
        statuses.put("rail-data-collector", healthCheckService::getRailDataCollectorHealth);
        statuses.put("geocoding-service", healthCheckService::getGeocodingServiceHealth);
        return CompositeReactiveHealthContributor.fromMap(statuses);
    }

    @Bean
    public ReactiveHealthContributor predictionDataCollectorServices() {
        Map<String, ReactiveHealthIndicator> statuses = new LinkedHashMap<>();
        statuses.put("weather-data-collector", healthCheckService::getWeatherDataCollectorHealth);
        return CompositeReactiveHealthContributor.fromMap(statuses);
    }

    @Bean
    public ReactiveHealthContributor delayPredictorService() {
        Map<String, ReactiveHealthIndicator> statuses = new LinkedHashMap<>();
        statuses.put("delay-predictor-service", healthCheckService::getDelayPredictorServiceHealth);
        return CompositeReactiveHealthContributor.fromMap(statuses);
    }
}