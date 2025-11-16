package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.common;

import com.github.benmanes.caffeine.cache.Cache;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.WeatherDataGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Primary
@Service
public class ReactiveHttpWeatherService implements WeatherService {

    private final WeatherDataGateway weatherGateway;
    private final Cache<String, WeatherInfo> weahterCache;

    @Autowired
    public ReactiveHttpWeatherService(@Qualifier("reactiveWeatherDataGateway") WeatherDataGateway weatherGateway, Cache<String, WeatherInfo> weahterCache) {
        this.weatherGateway = weatherGateway;
        this.weahterCache = weahterCache;
    }

    @Override
    public Mono<WeatherInfo> getWeather(String station, Double longitude, Double latitude, LocalDateTime dateTime) {
        WeatherInfo cached = weahterCache.getIfPresent(getCacheKey(station, longitude, latitude));
        if (cached != null) {
            log.debug("Weather cache hit for {}", getCacheKey(station, longitude, latitude));
            return Mono.just(cached);
        }

        log.debug("Geocoding cache miss for {}", getCacheKey(station, longitude, latitude));
        return weatherGateway.getWeatherInfo(station, longitude, latitude, dateTime)
                .doOnNext(response -> {
                    if (!isFallbackResponse(response)) {
                        log.debug("Weather response cached for {}", getCacheKey(station, longitude, latitude));
                        weahterCache.put(getCacheKey(station, longitude, latitude), response);
                    }
                });
    }

    private String getCacheKey(String station, Double longitude, Double latitude) {
        return Objects.toString(station) + ":" + Objects.toString(longitude) + ":" + Objects.toString(latitude);
    }

    private boolean isFallbackResponse(WeatherInfo weatherInfo) {
        return weatherInfo.getRain() == null && weatherInfo.getPrecipitation() == null;
    }
}
