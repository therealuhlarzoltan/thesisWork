package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.HealthCheckService;
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

    private final HealthCheckService healthService;

    @Bean
    public ReactiveHealthContributor dataCollectorServices() {
        final Map<String, ReactiveHealthIndicator> registry = new LinkedHashMap<>();
        registry.put("rail-data-collector", healthService::getRailDataCollectorHealth);
        registry.put("weather-data-collector", healthService::getWeatherDataCollectorHealth);
        registry.put("geocoding-service", healthService::getGeocodingServiceHealth);
        return CompositeReactiveHealthContributor.fromMap(registry);
    }
}
