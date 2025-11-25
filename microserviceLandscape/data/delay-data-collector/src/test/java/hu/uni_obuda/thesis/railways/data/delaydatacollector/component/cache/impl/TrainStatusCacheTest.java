package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.impl;

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

import static hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache.CACHE_PREFIX;
import static hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache.KEY_SET_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainStatusCacheTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOps;
    @Mock
    private ReactiveSetOperations<String, String> setOps;

    private TrainStatusCacheImpl testedObject;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        testedObject = new TrainStatusCacheImpl(redisTemplate);
        ReflectionTestUtils.setField(testedObject, "cacheDuration", 12);
    }

    private String expectedKey(String trainNumber, LocalDate date) {
        return CACHE_PREFIX + ":" + trainNumber + ":" + date;
    }

    @Test
    void isComplete_valueComplete_returnsTrue() {
        String trainNumber = "IC123";
        LocalDate date = LocalDate.of(2025, 1, 1);
        String key = expectedKey(trainNumber, date);

        when(valueOps.get(key)).thenReturn(Mono.just("complete"));

        StepVerifier.create(testedObject.isComplete(trainNumber, date))
                .expectNext(true)
                .verifyComplete();

        verify(valueOps).get(key);
    }

    @Test
    void isComplete_valueIncomplete_returnsFalse() {
        String trainNumber = "IC123";
        LocalDate date = LocalDate.of(2025, 1, 1);
        String key = expectedKey(trainNumber, date);

        when(valueOps.get(key)).thenReturn(Mono.just("incomplete"));

        StepVerifier.create(testedObject.isComplete(trainNumber, date))
                .expectNext(false)
                .verifyComplete();

        verify(valueOps).get(key);
    }

    @Test
    void isComplete_valueMissing_returnsFalse() {
        String trainNumber = "IC123";
        LocalDate date = LocalDate.of(2025, 1, 1);
        String key = expectedKey(trainNumber, date);

        when(valueOps.get(key)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.isComplete(trainNumber, date))
                .expectNext(false)
                .verifyComplete();

        verify(valueOps).get(key);
    }

    @Test
    void markComplete_called_storesValueAndRegistersKey() {
        String trainNumber = "IC123";
        LocalDate date = LocalDate.of(2025, 1, 1);

        when(valueOps.set(anyString(), eq("complete"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(setOps.add(eq(KEY_SET_PREFIX), anyString()))
                .thenReturn(Mono.just(1L));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        StepVerifier.create(testedObject.markComplete(trainNumber, date))
                .verifyComplete();

        verify(valueOps).set(keyCaptor.capture(), eq("complete"), any(Duration.class));
        String usedKey = keyCaptor.getValue();
        assertThat(usedKey).isEqualTo(expectedKey(trainNumber, date));

        verify(setOps).add(KEY_SET_PREFIX, usedKey);
    }

    @Test
    void markIncomplete_called_storesValueAndRegistersKey() {
        String trainNumber = "IC123";
        LocalDate date = LocalDate.of(2025, 1, 1);

        when(valueOps.set(anyString(), eq("incomplete"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(setOps.add(eq(KEY_SET_PREFIX), anyString()))
                .thenReturn(Mono.just(1L));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        StepVerifier.create(testedObject.markIncomplete(trainNumber, date))
                .verifyComplete();

        verify(valueOps).set(keyCaptor.capture(), eq("incomplete"), any(Duration.class));
        String usedKey = keyCaptor.getValue();
        assertThat(usedKey).isEqualTo(expectedKey(trainNumber, date));

        verify(setOps).add(KEY_SET_PREFIX, usedKey);
    }

    @Test
    void evict_called_removesBothValueAndKey() {
        String trainNumber = "IC123";
        LocalDate date = LocalDate.of(2025, 1, 1);
        String key = expectedKey(trainNumber, date);

        when(valueOps.delete(key)).thenReturn(Mono.just(true));
        when(setOps.remove(KEY_SET_PREFIX, key)).thenReturn(Mono.just(1L));

        StepVerifier.create(testedObject.evict(trainNumber, date))
                .verifyComplete();

        verify(valueOps).delete(key);
        verify(setOps).remove(KEY_SET_PREFIX, key);
    }

    @Test
    void evictAll_called_deletesAllValuesAndKeys() {
        String trainNumber1 = "IC123";
        String trainNumber2 = "IC456";
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 1, 2);

        String key1 = expectedKey(trainNumber1, date1);
        String key2 = expectedKey(trainNumber2, date2);

        when(setOps.members(KEY_SET_PREFIX)).thenReturn(Flux.just(key1, key2));
        when(redisTemplate.delete(any(Publisher.class))).thenReturn(Mono.just(2L));
        when(redisTemplate.delete(KEY_SET_PREFIX)).thenReturn(Mono.just(1L));

        StepVerifier.create(testedObject.evictAll())
                .verifyComplete();

        verify(setOps).members(KEY_SET_PREFIX);
        verify(redisTemplate).delete(any(Publisher.class));
        verify(redisTemplate).delete(KEY_SET_PREFIX);
    }

    @Test
    void evictAll_noKeysStored_doesNothing() {
        when(setOps.members(KEY_SET_PREFIX)).thenReturn(Flux.empty());

        StepVerifier.create(testedObject.evictAll())
                .verifyComplete();

        verify(setOps).members(KEY_SET_PREFIX);
    }
}
