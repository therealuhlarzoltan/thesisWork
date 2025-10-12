package hu.uni_obuda.thesis.railways.route.routeplannerservice.config;

import hu.uni_obuda.thesis.railways.route.routeplannerservice.health.DegradedHealthEndpointExtensionProcessor;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.health.DegradedHealthEndpointProcessor;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.health.DegradedReactiveHealthEndpointExtensionProcessor;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.health.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
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

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Bean
    public BeanPostProcessor healthEndpointWebExtensionPostProcessor() {
        return new DegradedHealthEndpointExtensionProcessor();
    }

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @Bean
    public BeanPostProcessor reactiveHealthEndpointWebExtensionPostProcessor() {
        return new DegradedReactiveHealthEndpointExtensionProcessor();
    }

    @ConditionalOnBean(HealthEndpoint.class)
    @Bean
    public BeanPostProcessor predictionEndpointWebExtensionPostProcessor() {
        return new DegradedHealthEndpointProcessor();
    }
}