package hu.uni_obuda.thesis.railways.route.routeplannerservice.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Value("${caching.geocoding.cache-duration:3}")
    private int geocodingCacheDuration;

    @Bean
    public Cache<String, GeocodingResponse> geocodingCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofHours(geocodingCacheDuration))
                .build();
    }
}
