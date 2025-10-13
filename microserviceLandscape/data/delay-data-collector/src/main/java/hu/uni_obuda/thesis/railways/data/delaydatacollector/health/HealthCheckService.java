package hu.uni_obuda.thesis.railways.data.delaydatacollector.health;

import org.springframework.boot.actuate.health.Health;
import reactor.core.publisher.Mono;

public interface HealthCheckService {
    Mono<Health> getRailDataCollectorHealth();

    Mono<Health> getWeatherDataCollectorHealth();

    Mono<Health> getGeocodingServiceHealth();
}
