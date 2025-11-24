package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.impl;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.reactivestreams.Publisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.CoordinatesCache.CACHE_PREFIX;
import static hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.CoordinatesCache.KEY_SET_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoordinatesCacheTest {

    @Mock
    private ReactiveRedisTemplate<String, GeocodingResponse> coordinatesRedisTemplate;
    @Mock
    private ReactiveRedisTemplate<String, String> keysRedisTemplate;
    @Mock
    private ReactiveValueOperations<String, GeocodingResponse> coordinatesValueOps;
    @Mock
    private ReactiveSetOperations<String, String> keysSetOps;

    private CoordinatesCacheImpl testedObject;

    @BeforeEach
    void setUp() {
        when(coordinatesRedisTemplate.opsForValue()).thenReturn(coordinatesValueOps);
        when(keysRedisTemplate.opsForSet()).thenReturn(keysSetOps);

        testedObject = new CoordinatesCacheImpl(coordinatesRedisTemplate, keysRedisTemplate);
    }

    @Test
    void isCached_valuePresent_returnsTrue() {
        String stationName = "Budapest-Keleti";
        String key = CACHE_PREFIX + ":" + stationName;

        when(coordinatesRedisTemplate.hasKey(key)).thenReturn(Mono.just(true));

        StepVerifier.create(testedObject.isCached(stationName))
                .expectNext(true)
                .verifyComplete();

        verify(coordinatesRedisTemplate).hasKey(key);
    }

    @Test
    void cache_called_storesValue() {
        String stationName = "Budapest-Keleti";
        GeocodingResponse coordinates = GeocodingResponse.builder()
                .latitude(47.5)
                .longitude(19.08)
                .address("Budapest, Keleti pályaudvar")
                .build();

        when(coordinatesValueOps.set(anyString(), eq(coordinates))).thenReturn(Mono.just(true));
        when(keysSetOps.add(eq(KEY_SET_PREFIX), anyString())).thenReturn(Mono.just(1L));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        StepVerifier.create(testedObject.cache(stationName, coordinates))
                .verifyComplete();

        verify(coordinatesValueOps).set(keyCaptor.capture(), eq(coordinates));
        String usedKey = keyCaptor.getValue();
        assertThat(usedKey).isEqualTo(CACHE_PREFIX + ":" + stationName);

        verify(keysSetOps).add(KEY_SET_PREFIX, usedKey);
    }

    @Test
    void get_valuePresent_returnsValue() {
        String stationName = "Budapest-Keleti";
        String key = CACHE_PREFIX + ":" + stationName;

        GeocodingResponse coordinates = GeocodingResponse.builder()
                .latitude(47.5)
                .longitude(19.08)
                .address("Budapest, Keleti pályaudvar")
                .build();

        when(coordinatesValueOps.get(key)).thenReturn(Mono.just(coordinates));

        StepVerifier.create(testedObject.get(stationName))
                .expectNext(coordinates)
                .verifyComplete();

        verify(coordinatesValueOps).get(key);
    }

    @Test
    void getAll_called_returnsAllValues() {
        String station1 = "Budapest-Keleti";
        String station2 = "Győr";

        String key1 = CACHE_PREFIX + ":" + station1;
        String key2 = CACHE_PREFIX + ":" + station2;

        GeocodingResponse c1 = GeocodingResponse.builder()
                .latitude(47.5)
                .longitude(19.08)
                .address("Budapest, Keleti pályaudvar")
                .build();

        GeocodingResponse c2 = GeocodingResponse.builder()
                .latitude(47.68)
                .longitude(17.64)
                .address("Győr, főpályaudvar")
                .build();

        when(keysSetOps.members(KEY_SET_PREFIX)).thenReturn(Flux.just(key1, key2));
        when(coordinatesValueOps.get(key1)).thenReturn(Mono.just(c1));
        when(coordinatesValueOps.get(key2)).thenReturn(Mono.just(c2));

        StepVerifier.create(testedObject.getAll())
                .expectNext(c1, c2)
                .verifyComplete();

        verify(keysSetOps).members(KEY_SET_PREFIX);
        verify(coordinatesValueOps).get(key1);
        verify(coordinatesValueOps).get(key2);
    }

    @Test
    void evict_called_removesBothValueAndKey() {
        String stationName = "Budapest-Keleti";
        String key = CACHE_PREFIX + ":" + stationName;

        when(coordinatesRedisTemplate.delete(key)).thenReturn(Mono.just(1L));
        when(keysSetOps.remove(KEY_SET_PREFIX, key)).thenReturn(Mono.just(1L));

        StepVerifier.create(testedObject.evict(stationName))
                .verifyComplete();

        verify(coordinatesRedisTemplate).delete(key);
        verify(keysSetOps).remove(KEY_SET_PREFIX, key);
    }

    @Test
    void evictAll_called_deletesAllValuesAndKeys() {
        String station1 = "Budapest-Keleti";
        String station2 = "Győr";

        String key1 = CACHE_PREFIX + ":" + station1;
        String key2 = CACHE_PREFIX + ":" + station2;

        when(keysSetOps.members(KEY_SET_PREFIX)).thenReturn(Flux.just(key1, key2));
        when(coordinatesRedisTemplate.delete(any(Publisher.class))).thenReturn(Mono.just(2L));
        when(keysRedisTemplate.delete(KEY_SET_PREFIX)).thenReturn(Mono.just(1L));

        StepVerifier.create(testedObject.evictAll())
                .verifyComplete();

        verify(keysSetOps).members(KEY_SET_PREFIX);
        verify(coordinatesRedisTemplate).delete(any(Publisher.class));
        verify(keysRedisTemplate).delete(KEY_SET_PREFIX);
    }

    @Test
    void evictAll_noKeysStored_doesNothing() {
        when(keysSetOps.members(KEY_SET_PREFIX)).thenReturn(Flux.empty());

        StepVerifier.create(testedObject.evictAll())
                .verifyComplete();

        verify(keysSetOps).members(KEY_SET_PREFIX);
    }
}
