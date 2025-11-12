package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ServiceResponseException;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
@Component
public class WeatherInfoRegistryImpl implements WeatherInfoRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherInfoRegistryImpl.class);

    private final Map<String, MonoSink<WeatherInfo>> pending = new ConcurrentHashMap<>();
    private final Map<String, Mono<WeatherInfo>> sharedMonos = new ConcurrentHashMap<>();

    private final WeatherInfoCache cache;

    @Value("${messaging.weather.response-event.wait-duration:30}")
    private Integer timeout;

    public Mono<WeatherInfo> waitForWeather(String stationName, LocalDateTime dateTime) {
        String key = stationName + ":" + dateTime.toString();
        LOG.info("Waiting for weather info with key {}", key);

        return sharedMonos.computeIfAbsent(key, k ->
                Mono.<WeatherInfo>create(sink -> pending.put(k, sink))
                        .timeout(Duration.ofSeconds(timeout))
                        .doFinally(signal -> {
                            pending.remove(k);
                            sharedMonos.remove(k);
                        })
                        .cache()
        );
    }

    public Mono<WeatherInfo> waitForWeather(String correlationId) {
        LOG.info("Waiting for weather info with correlationId {}", correlationId);
        return sharedMonos.computeIfAbsent(correlationId, k ->
                Mono.<WeatherInfo>create(sink -> pending.put(k, sink))
                        .timeout(Duration.ofSeconds(timeout))
                        .doFinally(signal -> {
                            pending.remove(k);
                            sharedMonos.remove(k);
                        })
                        .cache()
        );
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

    @Override
    public void onError(String stationName, LocalDateTime dateTime, Throwable throwable) {
        String key = stationName + ":" + dateTime.toString();
        MonoSink<WeatherInfo> sink = pending.remove(key);
        if (sink != null) {
            LOG.warn("Cancelling wait for weather info with key {} due to error: {}", key, throwable.getMessage());
            sink.error(new ServiceResponseException("Unable to get weather info", throwable));
        }
    }

    @Override
    public void onErrorWithCorrelationId(String correlationId, Throwable throwable) {
        MonoSink<WeatherInfo> sink = pending.remove(correlationId);
        if (sink != null) {
            LOG.warn("Cancelling wait for weather info with correlationId {} due to error: {}", correlationId, throwable.getMessage());
            sink.error(new ServiceResponseException("Unable to get weather info", throwable));
        }
    }
}
