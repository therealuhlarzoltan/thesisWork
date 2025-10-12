package hu.uni_obuda.thesis.railways.route.routeplannerservice.config;

import hu.uni_obuda.thesis.railways.route.routeplannerservice.health.HealthCheckService;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.health.PrimaryGroupStatusAggregator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    @Bean
    public HealthEndpointGroups degradablePrimaryHealthEndpointGroups(HealthEndpointGroups delegate) {
        StatusAggregator statusAggregatorBean = delegate.getPrimary().getStatusAggregator();
        StatusAggregator degradableStatusAggregator = new PrimaryGroupStatusAggregator(statusAggregatorBean);

        return new HealthEndpointGroups() {
            @Override
            public HealthEndpointGroup getPrimary() {
                HealthEndpointGroup original = delegate.getPrimary();
                return new HealthEndpointGroup() {
                    @Override
                    public boolean isMember(String name) {
                        return original.isMember(name);
                    }

                    @Override
                    public boolean showComponents(SecurityContext securityContext) {
                        return original.showComponents(securityContext);
                    }

                    @Override
                    public boolean showDetails(SecurityContext securityContext) {
                        return original.showDetails(securityContext);
                    }

                    @Override
                    public StatusAggregator getStatusAggregator() {
                        return degradableStatusAggregator;
                    }

                    @Override
                    public HttpCodeStatusMapper getHttpCodeStatusMapper() {
                        return original.getHttpCodeStatusMapper();
                    }

                    @Override
                    public AdditionalHealthEndpointPath getAdditionalPath() {
                        return original.getAdditionalPath();
                    }
                };
            }

            @Override
            public Set<String> getNames() {
                return delegate.getNames();
            }

            @Override
            public HealthEndpointGroup get(String name) {
                return delegate.get(name);
            }
        };
    }
}