package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.impl;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
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
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.WeatherInfoCache.CACHE_PREFIX;
import static hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.WeatherInfoCache.KEY_SET_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherInfoCacheTest {

    @Mock
    private ReactiveRedisTemplate<String, WeatherInfo> weatherInfosRedisTemplate;
    @Mock
    private ReactiveRedisTemplate<String, String> keysRedisTemplate;
    @Mock
    private ReactiveValueOperations<String, WeatherInfo> weatherValueOps;
    @Mock
    private ReactiveSetOperations<String, String> keysSetOps;

    private WeatherInfoCacheImpl testedObject;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    @BeforeEach
    void setUp() {
        when(weatherInfosRedisTemplate.opsForValue()).thenReturn(weatherValueOps);
        when(keysRedisTemplate.opsForSet()).thenReturn(keysSetOps);

        testedObject = new WeatherInfoCacheImpl(weatherInfosRedisTemplate, keysRedisTemplate);
        ReflectionTestUtils.setField(testedObject, "cacheDuration", 12);
    }

    private WeatherInfo createWeatherInfo() {
        return WeatherInfo.builder()
                .time(LocalDateTime.of(2025, 1, 1, 15, 30))
                .address("Budapest-Keleti")
                .temperature(10.5)
                .relativeHumidity(70.0)
                .build();
    }

    private String expectedKey(String stationName, LocalDateTime dateTime) {
        return CACHE_PREFIX + ":" + stationName + ":" + dateTime.format(FORMATTER);
    }

    @Test
    void isCached_valuePresent_returnsTrue() {
        String stationName = "Budapest-Keleti";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        String key = expectedKey(stationName, time);

        when(weatherInfosRedisTemplate.hasKey(key)).thenReturn(Mono.just(true));

        StepVerifier.create(testedObject.isCached(stationName, time))
                .expectNext(true)
                .verifyComplete();

        verify(weatherInfosRedisTemplate).hasKey(key);
    }

    @Test
    void isCached_valueMissing_returnsFalse() {
        String stationName = "Budapest-Keleti";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        String key = expectedKey(stationName, time);

        when(weatherInfosRedisTemplate.hasKey(key)).thenReturn(Mono.just(false));

        StepVerifier.create(testedObject.isCached(stationName, time))
                .expectNext(false)
                .verifyComplete();

        verify(weatherInfosRedisTemplate).hasKey(key);
    }

    @Test
    void cacheWeatherInfo_called_storesValueAndRegistersKey() {
        WeatherInfo weatherInfo = createWeatherInfo();
        String stationName = weatherInfo.getAddress();
        LocalDateTime time = weatherInfo.getTime();

        when(weatherValueOps.set(anyString(), eq(weatherInfo), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(keysSetOps.add(eq(KEY_SET_PREFIX), anyString()))
                .thenReturn(Mono.just(1L));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        StepVerifier.create(testedObject.cacheWeatherInfo(weatherInfo))
                .verifyComplete();

        verify(weatherValueOps).set(keyCaptor.capture(), eq(weatherInfo), any(Duration.class));
        String usedKey = keyCaptor.getValue();
        assertThat(usedKey).isEqualTo(expectedKey(stationName, time));

        verify(keysSetOps).add(KEY_SET_PREFIX, usedKey);
    }

    @Test
    void retrieveWeatherInfo_valuePresent_returnsValue() {
        String stationName = "Budapest-Keleti";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        String key = expectedKey(stationName, time);

        WeatherInfo weatherInfo = createWeatherInfo();

        when(weatherValueOps.get(key)).thenReturn(Mono.just(weatherInfo));

        StepVerifier.create(testedObject.retrieveWeatherInfo(stationName, time))
                .expectNext(weatherInfo)
                .verifyComplete();

        verify(weatherValueOps).get(key);
    }

    @Test
    void evict_called_removesBothValueAndKey() {
        String stationName = "Budapest-Keleti";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        String key = expectedKey(stationName, time);

        when(weatherValueOps.delete(key)).thenReturn(Mono.just(true));
        when(keysSetOps.remove(KEY_SET_PREFIX, key)).thenReturn(Mono.just(1L));

        StepVerifier.create(testedObject.evict(stationName, time))
                .verifyComplete();

        verify(weatherValueOps).delete(key);
        verify(keysSetOps).remove(KEY_SET_PREFIX, key);
    }

    @Test
    void evictAll_called_deletesAllValuesAndKeys() {
        String station1 = "Budapest-Keleti";
        String station2 = "Győr";
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 15, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 16, 0);

        String key1 = expectedKey(station1, time1);
        String key2 = expectedKey(station2, time2);

        when(keysSetOps.members(KEY_SET_PREFIX)).thenReturn(Flux.just(key1, key2));
        when(weatherInfosRedisTemplate.delete(any(Publisher.class))).thenReturn(Mono.just(2L));
        when(keysRedisTemplate.delete(KEY_SET_PREFIX)).thenReturn(Mono.just(1L));

        StepVerifier.create(testedObject.evictAll())
                .verifyComplete();

        verify(keysSetOps).members(KEY_SET_PREFIX);
        verify(weatherInfosRedisTemplate).delete(any(Publisher.class));
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
