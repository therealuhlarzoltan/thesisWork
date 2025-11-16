package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.common;

import com.github.benmanes.caffeine.cache.Cache;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.GeocodingGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.GeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Profile("data-source-emma")
@Primary
@Service
@Slf4j
public class ReactiveHttpGeocodingService implements GeocodingService {

    private final GeocodingGateway geocodingGateway;
    private final Cache<String, GeocodingResponse> geocodingCache;

    public ReactiveHttpGeocodingService(@Qualifier("reactiveGeocodingGateway") GeocodingGateway geocodingGateway, Cache<String, GeocodingResponse> geocodingCache) {
        this.geocodingGateway = geocodingGateway;
        this.geocodingCache = geocodingCache;
    }

    @Override
    public Mono<GeocodingResponse> getCoordinates(String stationName) {
        GeocodingResponse cached = geocodingCache.getIfPresent(stationName);
        if (cached != null) {
            log.debug("Geocoding cache hit for '{}'", stationName);
            return Mono.just(cached);
        }

        log.debug("Geocoding cache miss for '{}'", stationName);
        return geocodingGateway.getCoordinates(stationName)
                .doOnNext(response -> {
                    geocodingCache.put(stationName, response);
                    log.debug("Geocoding response cached for '{}'", stationName);
                });
    }
}
