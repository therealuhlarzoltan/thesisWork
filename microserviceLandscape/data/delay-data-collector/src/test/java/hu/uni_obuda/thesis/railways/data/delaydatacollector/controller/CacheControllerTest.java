package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction.CoordinatesCacheEvictor;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction.DelayCacheEvictor;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction.TrainStatusCacheEvictor;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction.WeatherCacheEvictor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheControllerTest {

    @Mock
    private DelayCacheEvictor delayCacheEvictor;
    @Mock
    private TrainStatusCacheEvictor trainStatusCacheEvictor;
    @Mock
    private WeatherCacheEvictor weatherCacheEvictor;
    @Mock
    private CoordinatesCacheEvictor coordinatesCacheEvictor;

    @InjectMocks
    private CacheControllerImpl testedObject;

    @Test
    void evictDelayInfoCache_callsDelayEvictorAndCompletes() {
        Mono<Void> result = testedObject.evictDelayInfoCache();

        StepVerifier.create(result)
                .verifyComplete();

        verify(delayCacheEvictor, times(1)).evict();
        verifyNoMoreInteractions(delayCacheEvictor, trainStatusCacheEvictor, weatherCacheEvictor, coordinatesCacheEvictor);
    }

    @Test
    void evictTrainStatusCache_callsTrainStatusEvictorAndCompletes() {
        Mono<Void> result = testedObject.evictTrainStatusCache();

        StepVerifier.create(result)
                .verifyComplete();

        verify(trainStatusCacheEvictor, times(1)).evict();
        verifyNoMoreInteractions(delayCacheEvictor, trainStatusCacheEvictor, weatherCacheEvictor, coordinatesCacheEvictor);
    }

    @Test
    void evictWeatherInfoCache_callsWeatherEvictorAndCompletes() {
        Mono<Void> result = testedObject.evictWeatherInfoCache();

        StepVerifier.create(result)
                .verifyComplete();

        verify(weatherCacheEvictor, times(1)).evict();
        verifyNoMoreInteractions(delayCacheEvictor, trainStatusCacheEvictor, weatherCacheEvictor, coordinatesCacheEvictor);
    }

    @Test
    void evictCoordinatesCache_callsCoordinatesEvictAndSaveAndCompletes() {
        Mono<Void> result = testedObject.evictCoordinatesCache();

        StepVerifier.create(result)
                .verifyComplete();

        verify(coordinatesCacheEvictor, times(1)).evictAndSave();
        verifyNoMoreInteractions(delayCacheEvictor, trainStatusCacheEvictor, weatherCacheEvictor, coordinatesCacheEvictor);
    }
}
