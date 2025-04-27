package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

import static java.util.logging.Level.FINE;

@RequiredArgsConstructor
@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    @Value("${app.rail-data-collector-url}")
    private String RAIL_DATA_COLLECTOR_URL;
    @Value("${app.weather-data-collector-url}")
    private String WEATHER_DATA_COLLECTOR_URL;
    @Value("${app.geocoding-service-url}")
    private String GEOCODING_SERVICE_URL;
    @Value("${app.health-check-path}")
    private String HEALTH_CHECK_PATH;

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private final WebClient webClient;

    @Override
    public Mono<Health> getRailDataCollectorHealth() {
        URI url = URI.create(RAIL_DATA_COLLECTOR_URL + HEALTH_CHECK_PATH);
        return checkHealth(url);
    }

    @Override
    public Mono<Health> getWeatherDataCollectorHealth() {
        URI url = URI.create(WEATHER_DATA_COLLECTOR_URL + HEALTH_CHECK_PATH);
        return checkHealth(url);
    }

    @Override
    public Mono<Health> getGeocodingServiceHealth() {
        URI url = URI.create(GEOCODING_SERVICE_URL + HEALTH_CHECK_PATH);
        return checkHealth(url);
    }

    private Mono<Health> checkHealth(URI url) {
        LOG.debug("Will call the Health API on URL: {}", url);
        return webClient.get().uri(url).retrieve().bodyToMono(String.class)
                .map(s -> new Health.Builder().up().build())
                .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
                .log(LOG.getName(), FINE);
    }
}
