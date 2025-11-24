package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.registry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.CoordinatesCache;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoordinatesRegistryTest {

    @Mock
    private CoordinatesCache coordinatesCache;

    @InjectMocks
    private CoordinatesRegistryImpl testedObject;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(testedObject, "timeout", 1);

        logger = (Logger) LoggerFactory.getLogger(CoordinatesRegistryImpl.class);
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

    private GeocodingResponse coords(String address, Double lat, Double lon) {
        return GeocodingResponse.builder()
                .address(address)
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    @Test
    void waitForCoordinates_createsSharedMonoAndLogs() {
        String station = "BPK";

        Mono<GeocodingResponse> mono = testedObject.waitForCoordinates(station);
        assertThat(mono).isNotNull();

        assertThat(sharedMonosMap()).containsKey(station);
        assertThat(pendingMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Waiting for coordinates for station " + station));
    }

    @Test
    void waitForCoordinates_success_onCoordinatesCompletesMono_cachesAndCleansMaps() {
        String station = "BPK";
        GeocodingResponse response = coords(station, 47.5, 19.08);

        when(coordinatesCache.cache(eq(station), eq(response))).thenReturn(Mono.empty());

        Mono<GeocodingResponse> mono = testedObject.waitForCoordinates(station);

        StepVerifier.create(mono)
                .then(() -> testedObject.onCoordinates(response))
                .expectNext(response)
                .verifyComplete();

        verify(coordinatesCache).cache(station, response);

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        List<String> messages = loggedMessages();
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Waiting for coordinates for station " + station));
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Registered coordinates for station " + station));
    }

    @Test
    void waitForCoordinates_emptyCoordinates_doesNotCacheButCompletesMono() {
        String station = "BPK";
        GeocodingResponse emptyCoords = coords(station, null, null);

        Mono<GeocodingResponse> mono = testedObject.waitForCoordinates(station);

        StepVerifier.create(mono)
                .then(() -> testedObject.onCoordinates(emptyCoords))
                .expectNext(emptyCoords)
                .verifyComplete();

        verify(coordinatesCache, never()).cache(any(), any());

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        List<String> messages = loggedMessages();
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Registered coordinates for station " + station));
    }

    @Test
    void waitForCoordinates_sharedMono_multipleSubscribersReceiveSameResult() {
        String station = "BPK";
        GeocodingResponse response = coords(station, 47.5, 19.08);

        when(coordinatesCache.cache(eq(station), eq(response))).thenReturn(Mono.empty());

        Mono<GeocodingResponse> mono1 = testedObject.waitForCoordinates(station);
        Mono<GeocodingResponse> mono2 = testedObject.waitForCoordinates(station);

        assertThat(mono1).isSameAs(mono2);

        StepVerifier.create(Mono.zip(mono1, mono2))
                .then(() -> testedObject.onCoordinates(response))
                .assertNext(tuple -> {
                    assertThat(tuple.getT1()).isEqualTo(response);
                    assertThat(tuple.getT2()).isEqualTo(response);
                })
                .verifyComplete();

        verify(coordinatesCache).cache(station, response);
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();
    }

    @Test
    void waitForCoordinates_timeout_emitsErrorAndCleansMaps() {
        String station = "BPK";

        StepVerifier.withVirtualTime(() -> testedObject.waitForCoordinates(station))
                .thenAwait(Duration.ofSeconds(2))
                .expectError()
                .verify();

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Waiting for coordinates for station " + station));
    }

    @Test
    void waitForCoordinatesWithCorrelationId_createsSharedMonoAndLogs() {
        String correlationId = "corr-123";

        Mono<GeocodingResponse> mono = testedObject.waitForCoordinatesWithCorrelationId(correlationId);
        assertThat(mono).isNotNull();

        assertThat(sharedMonosMap()).containsKey(correlationId);
        assertThat(pendingMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Waiting for coordinates with correlationId " + correlationId));
    }

    @Test
    void waitForCoordinatesWithCorrelationId_success_onCoordinatesWithCorrelationIdCompletesMonoAndCaches() {
        String correlationId = "corr-123";
        String station = "BPK";
        GeocodingResponse response = coords(station, 47.5, 19.08);

        when(coordinatesCache.cache(eq(station), eq(response))).thenReturn(Mono.empty());

        Mono<GeocodingResponse> mono = testedObject.waitForCoordinatesWithCorrelationId(correlationId);

        StepVerifier.create(mono)
                .then(() -> testedObject.onCoordinatesWithCorrelationId(correlationId, response))
                .expectNext(response)
                .verifyComplete();

        verify(coordinatesCache).cache(station, response);

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        List<String> messages = loggedMessages();
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Waiting for coordinates with correlationId " + correlationId));
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Registered coordinates with correlationId " + correlationId));
    }

    @Test
    void waitForCoordinatesWithCorrelationId_emptyCoordinates_doesNotCacheButCompletesMono() {
        String correlationId = "corr-123";
        String station = "BPK";
        GeocodingResponse emptyCoords = coords(station, null, null);

        Mono<GeocodingResponse> mono = testedObject.waitForCoordinatesWithCorrelationId(correlationId);

        StepVerifier.create(mono)
                .then(() -> testedObject.onCoordinatesWithCorrelationId(correlationId, emptyCoords))
                .expectNext(emptyCoords)
                .verifyComplete();

        verify(coordinatesCache, never()).cache(any(), any());
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();
    }

    @Test
    void onCoordinates_whenNoPendingSinkOnlyCachesAndLogs() {
        String station = "BPK";
        GeocodingResponse response = coords(station, 47.5, 19.08);

        when(coordinatesCache.cache(eq(station), eq(response))).thenReturn(Mono.empty());

        testedObject.onCoordinates(response);

        verify(coordinatesCache).cache(station, response);
        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(loggedMessages())
                .anyMatch(msg -> msg.contains("Registered coordinates for station " + station));
    }

    @Test
    void onError_station_propagatesServiceResponseExceptionToSubscriberAndCleansMaps() {
        String station = "BPK";
        RuntimeException cause = new RuntimeException("boom");

        Mono<GeocodingResponse> mono = testedObject.waitForCoordinates(station);

        StepVerifier.create(mono)
                .then(() -> testedObject.onError(station, cause))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ServiceResponseException.class);
                    assertThat(ex.getCause()).isEqualTo(cause);
                })
                .verify();

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        List<String> messages = loggedMessages();
        assertThat(messages)
                .anyMatch(msg -> msg.contains("Cancelling wait for coordinates for station " + station));
    }

    @Test
    void onError_station_whenNoPendingSink_doesNothing() {
        String station = "BPK";
        RuntimeException cause = new RuntimeException("boom");

        testedObject.onError(station, cause);

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(listAppender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    void onErrorWithCorrelationId_propagatesServiceResponseExceptionToSubscriberAndCleansMaps() {
        String correlationId = "corr-123";
        RuntimeException cause = new RuntimeException("boom");

        Mono<GeocodingResponse> mono = testedObject.waitForCoordinatesWithCorrelationId(correlationId);

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
                .anyMatch(msg -> msg.contains("Cancelling wait for coordinates with correlationId " + correlationId));
    }

    @Test
    void onErrorWithCorrelationId_whenNoPendingSink_doesNothing() {
        String correlationId = "corr-123";
        RuntimeException cause = new RuntimeException("boom");

        testedObject.onErrorWithCorrelationId(correlationId, cause);

        assertThat(pendingMap()).isEmpty();
        assertThat(sharedMonosMap()).isEmpty();

        assertThat(listAppender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }
}
