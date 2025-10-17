package hu.uni_obuda.thesis.railways.data.delaydatacollector.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

@Service
@Slf4j
@RequiredArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    @Value("${app.rail-data-collector-url}")
    private String railDataCollectorUrl;
    @Value("${app.weather-data-collector-url}")
    private String weatherDataCollectorUrl;
    @Value("${app.geocoding-service-url}")
    private String geocodingServiceUrl;
    @Value("${app.health-check-uri}")
    private String healthCheckUri;

    private final WebClient webClient;

    @Override
    public Mono<Health> getRailDataCollectorHealth() {
        String url = railDataCollectorUrl + healthCheckUri;
        return checkHealth(url, true);
    }

    @Override
    public Mono<Health> getWeatherDataCollectorHealth() {
        String url = weatherDataCollectorUrl + healthCheckUri;
        return checkHealth(url, false);
    }

    @Override
    public Mono<Health> getGeocodingServiceHealth() {
        String url = geocodingServiceUrl + healthCheckUri;
        return checkHealth(url, false);
    }

    private Mono<Health> checkHealth(String url, boolean isNecessary) {
        log.info("Will call the Health API on URL: {} with retries", url);
        return webClient.get().uri(url).retrieve().toBodilessEntity()
                .map(_ -> Health.up().build())
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                        .maxBackoff(Duration.ofSeconds(2))
                        .jitter(0.5)
                        .filter(this::shouldRetry)
                )
                .onErrorResume(ex -> {
                    if (isNecessary) {
                        return Mono.just(Health.down(ex).build());
                    } else {
                        return Mono.just(Health.outOfService().build());
                    }
                })
                .log(log.getName(), Level.FINE);
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            return true;
        }
        if (throwable instanceof WebClientRequestException) {
            return true;
        }
        if (throwable instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError();
        }
        return false;
    }
}