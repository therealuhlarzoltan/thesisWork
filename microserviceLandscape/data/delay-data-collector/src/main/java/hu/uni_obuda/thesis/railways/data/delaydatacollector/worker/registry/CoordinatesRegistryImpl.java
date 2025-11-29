package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.registry;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.CoordinatesCache;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ServiceResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Slf4j
@RequiredArgsConstructor
public class CoordinatesRegistryImpl implements CoordinatesRegistry {

    private final Map<String, MonoSink<GeocodingResponse>> pending = new ConcurrentHashMap<>();
    private final Map<String, Mono<GeocodingResponse>> sharedMonos = new ConcurrentHashMap<>();

    private final CoordinatesCache cache;

    @Value("${messaging.geocoding.response-event.wait-duration:30}")
    private Integer timeout;

    @Override
    public Mono<GeocodingResponse> waitForCoordinatesWithCorrelationId(String correlationId) {
        log.info("Waiting for coordinates with correlationId {}", correlationId);
        return sharedMonos.computeIfAbsent(correlationId, key ->
                Mono.<GeocodingResponse>create(sink -> pending.put(key, sink))
                        .timeout(Duration.ofSeconds(timeout))
                        .doFinally(_ -> {
                            pending.remove(key);
                            sharedMonos.remove(key);
                        })
                        .cache()
        );
    }

    @Override
    public Mono<GeocodingResponse> waitForCoordinates(String stationName) {
        log.info("Waiting for coordinates for station {}", stationName);
        return sharedMonos.computeIfAbsent(stationName, key ->
                Mono.<GeocodingResponse>create(sink -> pending.put(key, sink))
                        .timeout(Duration.ofSeconds(timeout))
                        .doFinally(signal -> {
                            pending.remove(key);
                            sharedMonos.remove(key);
                        })
                        .cache()
        );
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
        log.info("Registered coordinates for station {}", coordinates.getAddress());
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
        log.info("Registered coordinates with correlationId {}", correlationId);
    }

    @Override
    public void onError(String stationName, Throwable throwable) {
        MonoSink<GeocodingResponse> sink = pending.remove(stationName);
        if (sink != null) {
            log.warn("Cancelling wait for coordinates for station {} due to error: {}", stationName, throwable.getMessage());
            sink.error(new ServiceResponseException("Unable to get geocoding response", throwable));
        }
    }

    @Override
    public void onErrorWithCorrelationId(String correlationId, Throwable throwable) {
        MonoSink<GeocodingResponse> sink = pending.remove(correlationId);
        if (sink != null) {
            log.warn("Cancelling wait for coordinates with correlationId {} due to error: {}", correlationId, throwable.getMessage());
            sink.error(new ServiceResponseException("Unable to get geocoding response", throwable));
        }
    }
}
