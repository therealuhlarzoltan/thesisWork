package hu.uni_obuda.thesis.railways.route.routeplannerservice.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Value("${caching.geocoding.cache-duration-in-minutes:180}")
    private int geocodingCacheDuration;
    @Value("${caching.geocoding.cache-size:100}")
    private int geocodingCacheSize;
    @Value("${caching.weather.cache-duration-in-minutes:5}")
    private int weatherCacheDuration;
    @Value("${caching.weather.cache-size:100}")
    private int weatherCacheSize;
    @Value("${caching.route.cache-duration-in-seconds:30}")
    private int routeCacheDuration;
    @Value("${caching.route.cache-size:100}")
    private int routeCacheSize;



    @Bean
    public Cache<String, GeocodingResponse> geocodingCache() {
        return Caffeine.newBuilder()
                .maximumSize(geocodingCacheSize)
                .expireAfterWrite(Duration.ofMinutes(geocodingCacheDuration))
                .build();
    }

    @Bean
    public Cache<String, WeatherInfo> weatherCache() {
        return Caffeine.newBuilder()
                .maximumSize(weatherCacheSize)
                .expireAfterWrite(Duration.ofMinutes(weatherCacheDuration))
                .build();
    }


    @Bean
    public Cache<String, TrainRouteResponse> trainRouteCache() {
        return Caffeine.newBuilder()
                .maximumSize(routeCacheSize)
                .expireAfterWrite(Duration.ofSeconds(routeCacheDuration))
                .build();
    }
}
