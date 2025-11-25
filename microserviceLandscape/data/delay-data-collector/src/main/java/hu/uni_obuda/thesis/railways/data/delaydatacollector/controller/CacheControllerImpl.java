package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction.CoordinatesCacheEvictor;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction.DelayCacheEvictor;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction.TrainStatusCacheEvictor;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction.WeatherCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RequestMapping("cache")
@RestController
public class CacheControllerImpl implements CacheController {

    private final DelayCacheEvictor delayCacheEvictor;
    private final TrainStatusCacheEvictor trainStatusCacheEvictor;
    private final WeatherCacheEvictor weatherCacheEvictor;
    private final CoordinatesCacheEvictor coordinatesCacheEvictor;

    @Override
    public Mono<Void> evictDelayInfoCache() {
        return Mono.fromRunnable(delayCacheEvictor::evict);
    }

    @Override
    public Mono<Void> evictTrainStatusCache() {
        return Mono.fromRunnable(trainStatusCacheEvictor::evict);
    }

    @Override
    public Mono<Void> evictWeatherInfoCache() {
        return Mono.fromRunnable(weatherCacheEvictor::evict);
    }

    @Override
    public Mono<Void> evictCoordinatesCache() {
        return Mono.fromRunnable(coordinatesCacheEvictor::evictAndSave);
    }
}
