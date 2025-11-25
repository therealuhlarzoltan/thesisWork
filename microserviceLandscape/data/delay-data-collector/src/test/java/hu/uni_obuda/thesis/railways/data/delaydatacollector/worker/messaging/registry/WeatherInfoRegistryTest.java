package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.registry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ServiceResponseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherInfoRegistryTest {

    @Mock
    private WeatherInfoCache weatherInfoCache;

    @InjectMocks
    private WeatherInfoRegistryImpl testedObject;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(testedObject, "timeout", 1);

        logger = (Logger) LoggerFactory.getLogger(WeatherInfoRegistryImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> pendingMap() {
        return (Map<String, ?>) ReflectionTestUtils.getField(testedObject, "pending");
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> sharedMonosMap() {
        return (Map<String, ?>) ReflectionTestUtils.getField(testedObject, "sharedMonos");
    }

    private List<String> loggedMessages() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private WeatherInfo weather(String station, LocalDateTime time, Double temp) {
        return WeatherInfo.builder()
                .address(station)
                .time(time)
                .temperature(temp)
                .build();
    }

    @Test
    void waitForWeather_byKey_createsSharedMonoAndLogs() {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        String key = station + ":" + time.toString();

        Mono<WeatherInfo> mono = testedObject.waitForWeather(station, time);

        assertThat(mono).isNotNull();
        assertThat(sharedMonosMap()).containsKey(key);
        assertThat(pendingMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Waiting for weather info with key " + key));
    }

    @Test
    void waitForWeather_byKey_success_withTemperature_cachesAndCleansMaps() {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        String key = station + ":" + time.toString();
        WeatherInfo info = weather(station, time, 10.5);

        when(weatherInfoCache.cacheWeatherInfo(info)).thenReturn(Mono.empty());

        Mono<WeatherInfo> mono = testedObject.waitForWeather(station, time);

        StepVerifier.create(mono)
                .then(() -> {
                    assertThat(pendingMap()).containsKey(key);
                    testedObject.onWeatherInfo(info);
                })
                .expectNext(info)
                .verifyComplete();

        verify(weatherInfoCache).cacheWeatherInfo(info);

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        List<String> messages = loggedMessages();
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Waiting for weather info with key " + key));
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Received weather info with key " + key));
    }

    @Test
    void waitForWeather_byKey_success_withoutTemperature_doesNotCacheButCompletes() {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        String key = station + ":" + time.toString();
        WeatherInfo info = weather(station, time, null);

        Mono<WeatherInfo> mono = testedObject.waitForWeather(station, time);

        StepVerifier.create(mono)
                .then(() -> testedObject.onWeatherInfo(info))
                .expectNext(info)
                .verifyComplete();

        verify(weatherInfoCache, never()).cacheWeatherInfo(any());
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Received weather info with key " + key));
    }

    @Test
    void waitForWeather_byKey_sharedMono_multipleSubscribersSeeSameResult() {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 16, 0);
        String key = station + ":" + time.toString();
        WeatherInfo info = weather(station, time, 12.0);

        when(weatherInfoCache.cacheWeatherInfo(info)).thenReturn(Mono.empty());

        Mono<WeatherInfo> mono1 = testedObject.waitForWeather(station, time);
        Mono<WeatherInfo> mono2 = testedObject.waitForWeather(station, time);

        assertThat(mono1).isSameAs(mono2);
        assertThat(sharedMonosMap()).containsKey(key);

        StepVerifier.create(Mono.zip(mono1, mono2))
                .then(() -> testedObject.onWeatherInfo(info))
                .assertNext(tuple -> {
                    assertThat(tuple.getT1()).isEqualTo(info);
                    assertThat(tuple.getT2()).isEqualTo(info);
                })
                .verifyComplete();

        verify(weatherInfoCache).cacheWeatherInfo(info);
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();
    }

    @Test
    void waitForWeather_byKey_timeout_emitsErrorAndCleansMaps() {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 17, 0);
        String key = station + ":" + time.toString();

        StepVerifier.withVirtualTime(() -> testedObject.waitForWeather(station, time))
                .thenAwait(Duration.ofSeconds(2))
                .expectError()
                .verify();

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Waiting for weather info with key " + key));
    }

    @Test
    void onWeatherInfo_byKey_whenNoPendingOnlyCachesAndLogs() {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 18, 0);
        String key = station + ":" + time.toString();
        WeatherInfo info = weather(station, time, 13.0);

        when(weatherInfoCache.cacheWeatherInfo(info)).thenReturn(Mono.empty());

        testedObject.onWeatherInfo(info);

        verify(weatherInfoCache).cacheWeatherInfo(info);
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Received weather info with key " + key));
    }

    @Test
    void waitForWeather_byCorrelationId_createsSharedMonoAndLogs() {
        String correlationId = "corr-123";

        Mono<WeatherInfo> mono = testedObject.waitForWeather(correlationId);

        assertThat(mono).isNotNull();
        assertThat(sharedMonosMap()).containsKey(correlationId);
        assertThat(pendingMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Waiting for weather info with correlationId " + correlationId));
    }

    @Test
    void waitForWeather_byCorrelationId_success_withTemperature_cachesAndCleansMaps() {
        String correlationId = "corr-123";
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 19, 0);
        WeatherInfo info = weather(station, time, 14.0);

        when(weatherInfoCache.cacheWeatherInfo(info)).thenReturn(Mono.empty());

        Mono<WeatherInfo> mono = testedObject.waitForWeather(correlationId);

        StepVerifier.create(mono)
                .then(() -> testedObject.onWeatherInfo(correlationId, info))
                .expectNext(info)
                .verifyComplete();

        verify(weatherInfoCache).cacheWeatherInfo(info);
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        List<String> messages = loggedMessages();
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Waiting for weather info with correlationId " + correlationId));
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Received weather info with correlationId " + correlationId));
    }

    @Test
    void waitForWeather_byCorrelationId_success_withoutTemperature_doesNotCacheButCompletes() {
        String correlationId = "corr-123";
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 19, 0);
        WeatherInfo info = weather(station, time, null);

        Mono<WeatherInfo> mono = testedObject.waitForWeather(correlationId);

        StepVerifier.create(mono)
                .then(() -> testedObject.onWeatherInfo(correlationId, info))
                .expectNext(info)
                .verifyComplete();

        verify(weatherInfoCache, never()).cacheWeatherInfo(any());
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();
    }

    @Test
    void waitForWeather_byCorrelationId_sharedMono_multipleSubscribersSeeSameResult() {
        String correlationId = "corr-456";
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 20, 0);
        WeatherInfo info = weather(station, time, 15.0);

        when(weatherInfoCache.cacheWeatherInfo(info)).thenReturn(Mono.empty());

        Mono<WeatherInfo> mono1 = testedObject.waitForWeather(correlationId);
        Mono<WeatherInfo> mono2 = testedObject.waitForWeather(correlationId);

        assertThat(mono1).isSameAs(mono2);

        StepVerifier.create(Mono.zip(mono1, mono2))
                .then(() -> testedObject.onWeatherInfo(correlationId, info))
                .assertNext(tuple -> {
                    assertThat(tuple.getT1()).isEqualTo(info);
                    assertThat(tuple.getT2()).isEqualTo(info);
                })
                .verifyComplete();

        verify(weatherInfoCache).cacheWeatherInfo(info);
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();
    }

    @Test
    void onError_byKey_propagatesServiceResponseExceptionAndCleansMaps() {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 21, 0);
        String key = station + ":" + time.toString();
        RuntimeException cause = new RuntimeException("boom");

        Mono<WeatherInfo> mono = testedObject.waitForWeather(station, time);

        StepVerifier.create(mono)
                .then(() -> testedObject.onError(station, time, cause))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ServiceResponseException.class);
                    assertThat(ex.getCause()).isEqualTo(cause);
                })
                .verify();

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        List<String> messages = loggedMessages();
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Cancelling wait for weather info with key " + key));
    }

    @Test
    void onError_byKey_whenNoPending_doesNothingAndLogsNothing() {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 21, 0);
        RuntimeException cause = new RuntimeException("boom");

        testedObject.onError(station, time, cause);

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(listAppender.list)
                .noneMatch(e -> e.getLevel() == Level.WARN);
    }

    @Test
    void onErrorWithCorrelationId_propagatesServiceResponseExceptionAndCleansMaps() {
        String correlationId = "corr-err-1";
        RuntimeException cause = new RuntimeException("boom");

        Mono<WeatherInfo> mono = testedObject.waitForWeather(correlationId);

        StepVerifier.create(mono)
                .then(() -> testedObject.onErrorWithCorrelationId(correlationId, cause))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ServiceResponseException.class);
                    assertThat(ex.getCause()).isEqualTo(cause);
                })
                .verify();

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        List<String> messages = loggedMessages();
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Cancelling wait for weather info with correlationId " + correlationId));
    }

    @Test
    void onErrorWithCorrelationId_whenNoPending_doesNothingAndLogsNothing() {
        String correlationId = "corr-err-2";
        RuntimeException cause = new RuntimeException("boom");

        testedObject.onErrorWithCorrelationId(correlationId, cause);

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(listAppender.list)
                .noneMatch(e -> e.getLevel() == Level.WARN);
    }
}
