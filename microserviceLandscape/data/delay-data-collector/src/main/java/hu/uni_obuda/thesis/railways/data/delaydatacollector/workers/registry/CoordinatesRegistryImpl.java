package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.CoordinatesCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Component
public class CoordinatesRegistryImpl implements CoordinatesRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CoordinatesRegistryImpl.class);

    private final Map<String, MonoSink<GeocodingResponse>> pending = new ConcurrentHashMap<>();

    private final CoordinatesCache cache;

    @Value("${messaging.geocoding.response-event.wait-duration}:30")
    private Integer timeout;

    @Override
    public Mono<GeocodingResponse> waitForCoordinatesWithCorrelationId(String correlationId) {
        LOG.info("Waiting for coordinates with correlationId {}", correlationId);
        return Mono.<GeocodingResponse>create(sink -> pending.put(correlationId, sink))
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(TimeoutException.class, timeoutEx -> {
                    LOG.error("Geocoding response timed out with correlationId {}, no longer waiting for it", correlationId);
                    pending.remove(correlationId);
                });
    }

    @Override
    public Mono<GeocodingResponse> waitForCoordinates(String stationName) {
        LOG.info("Waiting for coordinates for station {}", stationName);
        return Mono.<GeocodingResponse>create(sink -> pending.put(stationName, sink))
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(TimeoutException.class, timeoutEx -> {
                    LOG.error("Geocoding response timed out with key {}, no longer waiting for it", stationName);
                    pending.remove(stationName);
                });
    }

    @Override
    public void onCoordinates(GeocodingResponse coordinates) {
        if (!coordinates.isEmpty()) {
            cache.cache(coordinates.getAddress(), coordinates).subscribe();
        }
        MonoSink<GeocodingResponse> sink = pending.remove(coordinates.getAddress());
        if (sink != null) {
            sink.success(coordinates);
        }
        LOG.info("Registered coordinates for station {}", coordinates.getAddress());
    }

    @Override
    public void onCoordinatesWithCorrelationId(String correlationId, GeocodingResponse coordinates) {
        if (!coordinates.isEmpty()) {
            cache.cache(coordinates.getAddress(), coordinates).subscribe();
        }
        MonoSink<GeocodingResponse> sink = pending.remove(correlationId);
        if (sink != null) {
            sink.success(coordinates);
        }
        LOG.info("Registered coordinates with correlationId {}", correlationId);
    }
}
