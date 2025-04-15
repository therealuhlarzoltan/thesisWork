package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WeatherInfoRegistryImpl implements WeatherInfoRegistry {

    private final Map<String, MonoSink<WeatherInfo>> pending = new ConcurrentHashMap<>();

    public Mono<WeatherInfo> waitForWeather(String stationName, LocalDateTime dateTime) {
        String key = stationName + ":" + dateTime.toString();
        return Mono.create(sink -> pending.put(key, sink));
    }

    public Mono<WeatherInfo> waitForWeather(String correlationId) {
        return Mono.create(sink -> pending.put(correlationId, sink));
    }

    public void onWeatherInfo(WeatherInfo info) {
        String key = info.getAddress() + ":" + info.getTime().toString();
        MonoSink<WeatherInfo> sink = pending.remove(key);
        if (sink != null) {
            sink.success(info);
        }
    }

    public void onWeatherInfo(String correlationId, WeatherInfo info) {
        MonoSink<WeatherInfo> sink = pending.remove(correlationId);
        if (sink != null) {
            sink.success(info);
        }
    }
}
