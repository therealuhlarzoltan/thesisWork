package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.CoordinatesCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.registry.CoordinatesRegistryImpl;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoordinateProcessorTest {

    private ObjectMapper objectMapper;
    private IncomingMessageSink incomingMessageSink;
    private CoordinatesRegistryImpl registry;

    @Mock
    private CoordinatesCache coordinatesCache;
    @Mock
    private HttpResponseEvent httpResponseEvent;
    @Mock
    private ResponsePayload responsePayload;

    private CoordinateProcessorImpl testedObject;

    private Logger processorLogger;
    private Logger registryLogger;
    private ListAppender<ILoggingEvent> processorAppender;
    private ListAppender<ILoggingEvent> registryAppender;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        incomingMessageSink = new IncomingMessageSink();
        registry = new CoordinatesRegistryImpl(coordinatesCache);
        ReflectionTestUtils.setField(registry, "timeout", 10);

        testedObject = new CoordinateProcessorImpl(objectMapper, registry, incomingMessageSink);

        processorLogger = (Logger) LoggerFactory.getLogger(CoordinateProcessorImpl.class);
        processorAppender = new ListAppender<>();
        processorAppender.start();
        processorLogger.addAppender(processorAppender);

        registryLogger = (Logger) LoggerFactory.getLogger(CoordinatesRegistryImpl.class);
        registryAppender = new ListAppender<>();
        registryAppender.start();
        registryLogger.addAppender(registryAppender);
    }

    @AfterEach
    void tearDown() {
        if (processorLogger != null && processorAppender != null) {
            processorLogger.detachAppender(processorAppender);
        }
        if (registryLogger != null && registryAppender != null) {
            registryLogger.detachAppender(registryAppender);
        }
    }

    private List<String> loggedProcessorMessages() {
        return processorAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private List<String> loggedRegistryMessages() {
        return registryAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private GeocodingResponse coords(String addr, double lat, double lon) {
        return GeocodingResponse.builder()
                .address(addr)
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    private Message messageWithoutCorrelation(Event<?, ?> payload) {
        return MessageBuilder.withPayload(payload).build();
    }

    private Message messageWithCorrelation(Event<?, ?> payload, String correlationId) {
        return MessageBuilder.withPayload(payload)
                .setHeader("correlationId", correlationId)
                .build();
    }

    @Test
    void accept_withoutCorrelation_success_emitsToSinkAndCompletesRegistryAndCaches() throws Exception {
        String station = "BPK";
        GeocodingResponse response = coords(station, 47.5, 19.08);
        String json = objectMapper.writeValueAsString(response);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);

        when(coordinatesCache.cache(eq(station), eq(response))).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        // subscribing to registry before processing
        final GeocodingResponse[] registryResult = new GeocodingResponse[1];
        CountDownLatch latch = new CountDownLatch(1);
        registry.waitForCoordinates(station)
                .subscribe(r -> {
                    registryResult[0] = r;
                    latch.countDown();
                });

        // verify sink + trigger processor
        StepVerifier.create(incomingMessageSink.getCoordinatesSink().asFlux().take(1))
                .then(() -> testedObject.accept(message))
                .assertNext(resp -> assertThat(resp).usingRecursiveComparison().isEqualTo(response))
                .verifyComplete();

        // registry got result
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(registryResult[0]).usingRecursiveComparison().isEqualTo(response);

        // cache invoked only once
        verify(coordinatesCache).cache(station, response);

        // correct logs
        List<String> processorLogs = loggedProcessorMessages();
        List<String> registryLogs = loggedRegistryMessages();
        assertThat(processorLogs).anyMatch(m -> m.contains("Processing message created at"));
        assertThat(registryLogs).anyMatch(m -> m.contains("Registered coordinates for station " + station));
    }

    @Test
    void accept_withoutCorrelation_error_logsErrorMessage_noSinkNoRegistry() {
        String rawError = "{invalid-json";

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.ERROR);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(rawError);

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        testedObject.accept(message);

        // sink should not emit anything
        StepVerifier.create(incomingMessageSink.getCoordinatesSink().asFlux().take(1))
                .expectTimeout(Duration.ofMillis(200))
                .verify();

        // no call into registry
        // using real registry, can asserting internal maps stay empty
        assertThat(ReflectionTestUtils.<Object>getField(registry, "pending"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .isEmpty();

        // correct logs
        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Received an error response: " + rawError));
    }

    @Test
    void accept_withoutCorrelation_unknownType_logsUnknown() {
        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(null);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn("{}");

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        testedObject.accept(message);

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Received unknown event type"));
    }

    @Test
    void accept_withCorrelation_success_emitsToSinkAndCompletesRegistryAndCaches() throws Exception {
        String station = "BPK";
        String correlationId = "corr-123";
        GeocodingResponse response = coords(station, 47.5, 19.08);
        String json = objectMapper.writeValueAsString(response);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);
        when(coordinatesCache.cache(eq(station), eq(response))).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = messageWithCorrelation(httpResponseEvent, correlationId);

        // waiting with correlationId
        final GeocodingResponse[] registryResult = new GeocodingResponse[1];
        CountDownLatch latch = new CountDownLatch(1);
        registry.waitForCoordinatesWithCorrelationId(correlationId)
                .subscribe(r -> {
                    registryResult[0] = r;
                    latch.countDown();
                });

        StepVerifier.create(incomingMessageSink.getCoordinatesSink().asFlux().take(1))
                .then(() -> testedObject.accept(message))
                .assertNext(resp -> assertThat(resp).usingRecursiveComparison().isEqualTo(response))
                .verifyComplete();

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(registryResult[0]).usingRecursiveComparison().isEqualTo(response);

        verify(coordinatesCache).cache(station, response);

        List<String> processorLogs = loggedProcessorMessages();
        List<String> registryLogs = loggedRegistryMessages();
        assertThat(processorLogs).anyMatch(m -> m.contains("Processing message created at"));
        assertThat(registryLogs).anyMatch(m -> m.contains("Registered coordinates with correlationId " + correlationId));
    }

    @Test
    void accept_withCorrelation_error_logsErrorMessage() {
        String correlationId = "corr-err";
        String rawError = "{invalid-json";

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.ERROR);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(rawError);

        Message<Event<?, ?>> message = messageWithCorrelation(httpResponseEvent, correlationId);

        testedObject.accept(message);

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Received an error response: " + rawError));
    }

    @Test
    void accept_withCorrelation_unknownType_logsUnknown() {
        String correlationId = "corr-unknown";

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(null);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn("{}");

        Message<Event<?, ?>> message = messageWithCorrelation(httpResponseEvent, correlationId);

        testedObject.accept(message);

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Received unknown event type"));
    }

    @Test
    void accept_whenPayloadNotHttpResponseEvent_logsUnexpectedAndReturns() {
        @SuppressWarnings("unchecked")
        Event<String, String> genericEvent = mock(Event.class);

        Message<Event<?, ?>> message = messageWithoutCorrelation(genericEvent);

        testedObject.accept(message);

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Unexpected event parameters, expected a HttpResponseEvent"));
        assertThat(logs).anyMatch(m -> m.contains("Could not retrieve response event from message"));
    }

    @Test
    void accept_whenGeocodingDeserializationFails_logsErrorAndDoesNotEmit() {
        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn("{not-valid-json");

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        testedObject.accept(message);

        // no sink emissions
        StepVerifier.create(incomingMessageSink.getCoordinatesSink().asFlux().take(1))
                .expectTimeout(Duration.ofMillis(200))
                .verify();

        List<String> processorLogs = loggedProcessorMessages();
        List<String> registryLogs = loggedRegistryMessages();
        assertThat(processorLogs).anyMatch(m -> m.contains("Could not deserialize object from json"));
        assertThat(registryLogs).noneMatch(m -> m.contains("Registered coordinates"));
    }

    @Test
    void concurrentWaiters_allReceiveSameResponse_registryAndCacheCorrect() throws Exception {
        String station = "BPK";
        GeocodingResponse response = coords(station, 47.5, 19.08);
        String json = objectMapper.writeValueAsString(response);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);
        when(coordinatesCache.cache(eq(station), eq(response))).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<GeocodingResponse> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() ->
                    registry.waitForCoordinates(station)
                            .subscribe(r -> {
                                results.add(r);
                                latch.countDown();
                            }));
        }

        // giving some time for subscribers to register pending sinks
        Thread.sleep(100);

        // processing the message in main thread
        testedObject.accept(message);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(results).hasSize(threads);
        results.forEach(r -> assertThat(r).usingRecursiveComparison().isEqualTo(response));

        // single cache invocation for multiple identical calls
        verify(coordinatesCache, times(1)).cache(station, response);
    }

    @Test
    void happyPath_noErrorLogs() throws Exception {
        String station = "BPK2";
        GeocodingResponse response = coords(station, 47.1, 19.02);
        String json = objectMapper.writeValueAsString(response);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);
        when(coordinatesCache.cache(eq(station), eq(response))).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        testedObject.accept(message);

        assertThat(processorAppender.list)
                .noneMatch(e -> e.getLevel() == Level.ERROR);
    }
}
