package hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.impl;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.TimetableCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.List;

import static hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.TimetableCache.KEY_SET_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmmaTimetableCacheTest {

    @Mock
    private ReactiveRedisTemplate<String, EmmaShortTimetableResponse> timetableRedisTemplate;
    @Mock
    private ReactiveRedisTemplate<String, String> keysRedisTemplate;
    @Mock
    private ReactiveValueOperations<String, EmmaShortTimetableResponse> timetableValueOps;
    @Mock
    private ReactiveSetOperations<String, String> keysSetOps;

    private EmmaTimetableCacheImpl testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new EmmaTimetableCacheImpl(timetableRedisTemplate, keysRedisTemplate);
        ReflectionTestUtils.setField(testedObject, "cacheDuration", 6);
    }

    @Test
    void isCached_shouldDelegateToRedisWithProperKey() {
        String from = "BUDAPEST";
        String to = "GYOR";
        LocalDate date = LocalDate.of(2025, 1, 1);

        String expectedKey = TimetableCache.CACHE_PREFIX + ":" + from + ":" + to + ":" + date;

        when(timetableRedisTemplate.hasKey(expectedKey)).thenReturn(Mono.just(true));

        Mono<Boolean> result = testedObject.isCached(from, to, date);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(timetableRedisTemplate, times(1)).hasKey(expectedKey);
        verifyNoMoreInteractions(timetableRedisTemplate);
        verifyNoInteractions(keysRedisTemplate);
    }

    @Test
    void cache_shouldStoreTimetableAndRegisterKeyWithExpiration() {
        String from = "BUDAPEST";
        String to = "DEBRECEN";
        LocalDate date = LocalDate.of(2025, 1, 2);
        String expectedKey = TimetableCache.CACHE_PREFIX + ":" + from + ":" + to + ":" + date;

        EmmaShortTimetableResponse timetable = new EmmaShortTimetableResponse();

        when(timetableRedisTemplate.opsForValue()).thenReturn(timetableValueOps);
        when(keysRedisTemplate.opsForSet()).thenReturn(keysSetOps);

        when(timetableValueOps.set(eq(expectedKey), eq(timetable), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(keysSetOps.add(KEY_SET_PREFIX, expectedKey)).thenReturn(Mono.just(1L));

        Mono<Void> result = testedObject.cache(from, to, date, timetable);

        StepVerifier.create(result)
                .verifyComplete();

        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(timetableRedisTemplate).opsForValue();
        verify(timetableValueOps).set(eq(expectedKey), eq(timetable), durationCaptor.capture());

        Duration usedDuration = durationCaptor.getValue();
        assertThat(usedDuration).isEqualTo(Duration.ofHours(6));

        verify(keysRedisTemplate).opsForSet();
        verify(keysSetOps).add(KEY_SET_PREFIX, expectedKey);

        verifyNoMoreInteractions(timetableRedisTemplate, keysRedisTemplate, timetableValueOps, keysSetOps);
    }

    @Test
    void get_keyPresent_shouldReturnTimetableFromCache() {
        String from = "BUDAPEST";
        String to = "VAC";
        LocalDate date = LocalDate.of(2025, 1, 3);
        String expectedKey = TimetableCache.CACHE_PREFIX + ":" + from + ":" + to + ":" + date;

        EmmaShortTimetableResponse expectedResponse = new EmmaShortTimetableResponse();

        when(timetableRedisTemplate.opsForValue()).thenReturn(timetableValueOps);
        when(timetableValueOps.get(expectedKey)).thenReturn(Mono.just(expectedResponse));

        Mono<EmmaShortTimetableResponse> result = testedObject.get(from, to, date);

        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(timetableRedisTemplate).opsForValue();
        verify(timetableValueOps).get(expectedKey);
        verifyNoMoreInteractions(timetableRedisTemplate, timetableValueOps);
        verifyNoInteractions(keysRedisTemplate, keysSetOps);
    }

    @Test
    void evictAll_noKeyPresent_shouldNotDoAnything() {
        when(keysRedisTemplate.opsForSet()).thenReturn(keysSetOps);
        when(keysSetOps.members(KEY_SET_PREFIX)).thenReturn(Flux.empty());

        Mono<Void> result = testedObject.evictAll();

        StepVerifier.create(result)
                .verifyComplete();

        verify(keysRedisTemplate).opsForSet();
        verify(keysSetOps).members(KEY_SET_PREFIX);

        verify(timetableRedisTemplate, never()).delete(any(Publisher.class));
        verify(keysRedisTemplate, never()).delete(anyString());

        verifyNoMoreInteractions(keysRedisTemplate, keysSetOps, timetableRedisTemplate);
    }

    @Test
    void evictAll_keysPresent_shouldDeleteAll() {
        List<String> keys = List.of(
                "timetableResponse:A:B:2025-01-01",
                "timetableResponse:C:D:2025-01-01"
        );

        when(keysRedisTemplate.opsForSet()).thenReturn(keysSetOps);
        when(keysSetOps.members(KEY_SET_PREFIX)).thenReturn(Flux.fromIterable(keys));

        when(timetableRedisTemplate.delete(any(Publisher.class))).thenReturn(Mono.just((long) keys.size()));
        when(keysRedisTemplate.delete(KEY_SET_PREFIX)).thenReturn(Mono.just(1L));

        Mono<Void> result = testedObject.evictAll();

        StepVerifier.create(result)
                .verifyComplete();

        verify(keysRedisTemplate).opsForSet();
        verify(keysSetOps).members(KEY_SET_PREFIX);

        ArgumentCaptor<Publisher<String>> publisherCaptor = ArgumentCaptor.forClass(Publisher.class);
        verify(timetableRedisTemplate).delete(publisherCaptor.capture());
        Publisher<String> publisherUsed = publisherCaptor.getValue();
        assertThat(publisherUsed).isNotNull();

        verify(keysRedisTemplate).delete(KEY_SET_PREFIX);

        verifyNoMoreInteractions(keysRedisTemplate, keysSetOps, timetableRedisTemplate);
    }
}
