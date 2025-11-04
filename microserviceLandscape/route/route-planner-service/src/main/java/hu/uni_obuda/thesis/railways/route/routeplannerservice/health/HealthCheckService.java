package hu.uni_obuda.thesis.railways.route.routeplannerservice.health;

import org.springframework.boot.actuate.health.Health;
import reactor.core.publisher.Mono;

public interface HealthCheckService {
    Mono<Health> getDelayDataCollectorHealth();
    Mono<Health> getRailDataCollectorHealth();
    Mono<Health> getWeatherDataCollectorHealth();
    Mono<Health> getGeocodingServiceHealth();
    Mono<Health> getDelayPredictorServiceHealth();
}
