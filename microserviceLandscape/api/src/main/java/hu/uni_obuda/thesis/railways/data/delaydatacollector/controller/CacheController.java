package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import reactor.core.publisher.Mono;

public interface CacheController {

    @DeleteMapping("delay")
    Mono<Void> evictDelayInfoCache();

    @DeleteMapping("train")
    Mono<Void> evictTrainStatusCache();

    @DeleteMapping("weather")
    Mono<Void> evictWeatherInfoCache();

    @DeleteMapping("coordinates")
    Mono<Void> evictCoordinatesCache();
}
