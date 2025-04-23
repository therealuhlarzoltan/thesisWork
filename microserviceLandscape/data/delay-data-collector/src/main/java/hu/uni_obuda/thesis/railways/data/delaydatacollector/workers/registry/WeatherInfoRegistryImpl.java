package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Component
public class WeatherInfoRegistryImpl implements WeatherInfoRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherInfoRegistryImpl.class);

    private final Map<String, MonoSink<WeatherInfo>> pending = new ConcurrentHashMap<>();

    private final WeatherInfoCache cache;

    @Value("${messaging.weather.response-event.wait-duration}:30")
    private Integer timeout;

    public Mono<WeatherInfo> waitForWeather(String stationName, LocalDateTime dateTime) {
        String key = stationName + ":" + dateTime.toString();
        LOG.info("Waiting for weather info with key {}", key);
        return Mono.<WeatherInfo>create(sink -> pending.put(key, sink))
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(TimeoutException.class, timeoutEx -> {
                    LOG.error("Weather info response timed out with key {}, no longer waiting for it", key);
                    pending.remove(key);
                });
    }

    public Mono<WeatherInfo> waitForWeather(String correlationId) {
        LOG.info("Waiting for weather info with correlationId {}", correlationId);
        return Mono.<WeatherInfo>create(sink -> pending.put(correlationId, sink))
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(TimeoutException.class, timeoutEx -> {
                    LOG.error("Weather info response timed out with correlationId {}, no longer waiting for it", correlationId);
                    pending.remove(correlationId);
                });
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
