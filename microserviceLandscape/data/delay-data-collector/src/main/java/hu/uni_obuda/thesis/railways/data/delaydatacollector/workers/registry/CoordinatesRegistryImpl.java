package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.CoordinatesCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class CoordinatesRegistryImpl implements CoordinatesRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CoordinatesRegistryImpl.class);

    private final Map<String, MonoSink<GeocodingResponse>> pending = new ConcurrentHashMap<>();

    private final CoordinatesCache cache;

    @Override
    public Mono<GeocodingResponse> waitForCoordinatesWithCorrelationId(String correlationId) {
        LOG.info("Waiting for coordinates with correlationId {}", correlationId);
        return Mono.create(sink -> pending.put(correlationId, sink));
    }

    @Override
    public Mono<GeocodingResponse> waitForCoordinates(String stationName) {
        LOG.info("Waiting for coordinates for station {}", stationName);
        return Mono.create(sink -> pending.put(stationName, sink));
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
