package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class WeatherInfoRegistryImpl implements WeatherInfoRegistry {

    private static final Logger LOG = LogManager.getLogger(WeatherInfoRegistryImpl.class);

    private final Map<String, MonoSink<WeatherInfo>> pending = new ConcurrentHashMap<>();

    private final WeatherInfoCache cache;

    public Mono<WeatherInfo> waitForWeather(String stationName, LocalDateTime dateTime) {
        String key = stationName + ":" + dateTime.toString();
        LOG.info("Waiting for weather info with key {}", key);
        return Mono.create(sink -> pending.put(key, sink));
    }

    public Mono<WeatherInfo> waitForWeather(String correlationId) {
        LOG.info("Waiting for weather info with correlationId {}", correlationId);
        return Mono.create(sink -> pending.put(correlationId, sink));
    }

    public void onWeatherInfo(WeatherInfo info) {
        String key = info.getAddress() + ":" + info.getTime().toString();
        if (info.getTemperature() != null) {
            cache.cacheWeatherInfo(info).subscribe();
        }
        MonoSink<WeatherInfo> sink = pending.remove(key);
        if (sink != null) {
            sink.success(info);
        }
        LOG.info("Received weather info with key {}", key);
    }

    public void onWeatherInfo(String correlationId, WeatherInfo info) {
        if (info.getTemperature() != null) {
            cache.cacheWeatherInfo(info).subscribe();
        }
        MonoSink<WeatherInfo> sink = pending.remove(correlationId);
        if (sink != null) {
            sink.success(info);
        }
        LOG.info("Received weather info with correlationId {}", correlationId);
    }
}
