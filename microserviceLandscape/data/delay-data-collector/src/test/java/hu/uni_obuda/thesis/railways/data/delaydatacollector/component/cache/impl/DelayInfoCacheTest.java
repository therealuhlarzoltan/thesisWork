package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.impl;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
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
import java.time.LocalDate;

import static hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.DelayInfoCache.CACHE_PREFIX;
import static hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.DelayInfoCache.KEY_SET_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DelayInfoCacheTest {

    @Mock
    private ReactiveRedisTemplate<String, String> delaysRedisTemplate;
    @Mock
    private ReactiveRedisTemplate<String, String> keysRedisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> delaysValueOps;
    @Mock
    private ReactiveSetOperations<String, String> keysSetOps;

    private DelayInfoCacheImpl testedObject;

    @BeforeEach
    void setUp() {
        when(delaysRedisTemplate.opsForValue()).thenReturn(delaysValueOps);
        when(keysRedisTemplate.opsForSet()).thenReturn(keysSetOps);

        testedObject = new DelayInfoCacheImpl(delaysRedisTemplate, keysRedisTemplate);
        ReflectionTestUtils.setField(testedObject, "cacheDuration", 6);
    }

    private DelayInfo createDelay() {
        return DelayInfo.builder()
                .trainNumber("IC123")
                .stationCode("BPK")
                .date(LocalDate.of(2025, 1, 1))
                .build();
    }

    private String expectedKey(DelayInfo delay) {
        return CACHE_PREFIX + ":" +
                delay.getTrainNumber() + ":" +
                delay.getStationCode() + ":" +
                delay.getDate().toEpochDay();
    }

    @Test
    void isDuplicate_valuePresent_returnsTrue() {
        DelayInfo delay = createDelay();
        String key = expectedKey(delay);

        when(delaysValueOps.get(key)).thenReturn(Mono.just("1"));

        StepVerifier.create(testedObject.isDuplicate(delay))
                .expectNext(true)
                .verifyComplete();

        verify(delaysValueOps).get(key);
    }

    @Test
    void isDuplicate_valueMissing_returnsFalse() {
        DelayInfo delay = createDelay();
        String key = expectedKey(delay);

        when(delaysValueOps.get(key)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.isDuplicate(delay))
                .expectNext(false)
                .verifyComplete();

        verify(delaysValueOps).get(key);
    }

    @Test
    void cacheDelay_called_storesValueAndRegistersKey() {
        DelayInfo delay = createDelay();

        when(delaysValueOps.set(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(keysSetOps.add(eq(KEY_SET_PREFIX), anyString()))
                .thenReturn(Mono.just(1L));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        StepVerifier.create(testedObject.cacheDelay(delay))
                .verifyComplete();

        verify(delaysValueOps).set(keyCaptor.capture(), eq("1"), any(Duration.class));
        String usedKey = keyCaptor.getValue();
        assertThat(usedKey).isEqualTo(expectedKey(delay));

        verify(keysSetOps).add(KEY_SET_PREFIX, usedKey);
    }

    @Test
    void evict_called_removesBothValueAndKey() {
        DelayInfo delay = createDelay();
        String key = expectedKey(delay);

        when(delaysRedisTemplate.delete(key)).thenReturn(Mono.just(1L));
        when(keysSetOps.remove(KEY_SET_PREFIX, key)).thenReturn(Mono.just(1L));

        StepVerifier.create(testedObject.evict(delay))
                .verifyComplete();

        verify(delaysRedisTemplate).delete(key);
        verify(keysSetOps).remove(KEY_SET_PREFIX, key);
    }

    @Test
    void evictAll_called_deletesAllValuesAndKeys() {
        DelayInfo d1 = createDelay();
        DelayInfo d2 = DelayInfo.builder()
                .trainNumber("IC456")
                .stationCode("GYO")
                .date(LocalDate.of(2025, 1, 2))
                .build();

        String key1 = expectedKey(d1);
        String key2 = expectedKey(d2);

        when(keysSetOps.members(KEY_SET_PREFIX)).thenReturn(Flux.just(key1, key2));
        when(delaysRedisTemplate.delete(any(Publisher.class))).thenReturn(Mono.just(2L));
        when(keysRedisTemplate.delete(KEY_SET_PREFIX)).thenReturn(Mono.just(1L));

        StepVerifier.create(testedObject.evictAll())
                .verifyComplete();

        verify(keysSetOps).members(KEY_SET_PREFIX);
        verify(delaysRedisTemplate).delete(any(Publisher.class));
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
