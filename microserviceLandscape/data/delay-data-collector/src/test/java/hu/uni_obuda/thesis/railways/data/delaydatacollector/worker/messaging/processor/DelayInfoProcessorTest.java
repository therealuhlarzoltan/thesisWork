package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DelayInfoProcessorTest {

    private ObjectMapper objectMapper;
    private IncomingMessageSink incomingMessageSink;

    @Mock
    private TrainStatusCache statusCache;
    @Mock
    private HttpResponseEvent httpResponseEvent;
    @Mock
    private ResponsePayload responsePayload;

    private DelayInfoProcessorImpl testedObject;

    private Logger processorLogger;
    private ListAppender<ILoggingEvent> processorAppender;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        incomingMessageSink = new IncomingMessageSink();

        testedObject = new DelayInfoProcessorImpl(objectMapper, incomingMessageSink, statusCache);

        processorLogger = (Logger) LoggerFactory.getLogger(DelayInfoProcessorImpl.class);
        processorAppender = new ListAppender<>();
        processorAppender.start();
        processorLogger.addAppender(processorAppender);
    }

    @AfterEach
    void tearDown() {
        if (processorLogger != null && processorAppender != null) {
            processorLogger.detachAppender(processorAppender);
        }
    }

    private List<String> loggedProcessorMessages() {
        return processorAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private DelayInfo delayInfo(String train, String station) {
        return DelayInfo.builder()
                .trainNumber(train)
                .stationCode(station)
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
    void accept_withoutCorrelation_success_emitsToSinkAndLogs() throws Exception {
        String train = "IC123";
        String station = "BPK";
        DelayInfo info = delayInfo(train, station);
        String json = objectMapper.writeValueAsString(info);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        StepVerifier.create(incomingMessageSink.getDelaySink().asFlux().take(1))
                .then(() -> testedObject.accept(message))
                .assertNext(resp -> assertThat(resp).usingRecursiveComparison().isEqualTo(info))
                .verifyComplete();

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Processing message created at"));
        assertThat(logs).anyMatch(m -> m.contains("Added delay info"));
    }

    @Test
    void accept_withCorrelation_success_emitsToSinkAndLogs() throws Exception {
        String train = "IC123";
        String station = "BPK";
        String correlationId = "corr-123";
        DelayInfo info = delayInfo(train, station);
        String json = objectMapper.writeValueAsString(info);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);

        Message<Event<?, ?>> message = messageWithCorrelation(httpResponseEvent, correlationId);

        StepVerifier.create(incomingMessageSink.getDelaySink().asFlux().take(1))
                .then(() -> testedObject.accept(message))
                .assertNext(resp -> assertThat(resp).usingRecursiveComparison().isEqualTo(info))
                .verifyComplete();

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Processing message created at"));
        assertThat(logs).anyMatch(m -> m.contains("Added delay info to message sink for train " + train));
    }

    @Test
    void accept_withoutCorrelation_errorWithTooEarly_marksCompleteAndLogs() throws Exception {
        String train = "IC999";
        LocalDate date = LocalDate.of(2025, 1, 1);
        String json = "{\"trainNumber\":\"" + train + "\",\"date\":\"" + date + "\"}";

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.ERROR);
        when(httpResponseEvent.getKey()).thenReturn("some-key");
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getStatus()).thenReturn(HttpStatus.TOO_EARLY);
        when(responsePayload.getMessage()).thenReturn(json);
        when(statusCache.markComplete(train, date)).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        testedObject.accept(message);

        verify(statusCache).markComplete(train, date);

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Received an error response for id: some-key"));
        assertThat(logs).anyMatch(m -> m.contains("Train with number " + train + " is not in operation on " + date));
    }

    @Test
    void accept_withCorrelation_errorWithTooEarly_marksCompleteAndLogs() throws Exception {
        String train = "IC999";
        LocalDate date = LocalDate.of(2025, 1, 1);
        String correlationId = "corr-too-early";
        String json = "{\"trainNumber\":\"" + train + "\",\"date\":\"" + date + "\"}";

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.ERROR);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getStatus()).thenReturn(HttpStatus.TOO_EARLY);
        when(responsePayload.getMessage()).thenReturn(json);
        when(statusCache.markComplete(train, date)).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = messageWithCorrelation(httpResponseEvent, correlationId);

        testedObject.accept(message);

        verify(statusCache).markComplete(train, date);

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Received an error response for correlationId: " + correlationId));
        assertThat(logs).anyMatch(m -> m.contains("Train with number " + train + " is not in operation on " + date));
    }

    @Test
    void accept_errorWithNonTooEarlyStatus_doesNotMarkComplete() {
        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.ERROR);
        when(httpResponseEvent.getKey()).thenReturn("some-key");
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(responsePayload.getMessage()).thenReturn("{\"message\":\"oops\"}");

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        testedObject.accept(message);

        verify(statusCache, never()).markComplete(anyString(), any());
        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Received an error response for id: some-key"));
    }

    @Test
    void accept_withCorrelation_errorWithNonTooEarlyStatus_doesNotMarkComplete() {
        String correlationId = "corr-err";

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.ERROR);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);
        when(responsePayload.getMessage()).thenReturn("{\"message\":\"oops\"}");

        Message<Event<?, ?>> message = messageWithCorrelation(httpResponseEvent, correlationId);

        testedObject.accept(message);

        verify(statusCache, never()).markComplete(anyString(), any());
        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Received an error response for correlationId: " + correlationId));
    }

    @Test
    void accept_unknownType_logsUnknown() {
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
    void accept_whenDelayInfoDeserializationFails_logsErrorAndDoesNotEmit() {
        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn("{not-valid-json");

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        testedObject.accept(message);

        StepVerifier.create(incomingMessageSink.getDelaySink().asFlux().take(1))
                .expectTimeout(Duration.ofMillis(200))
                .verify();

        List<String> logs = loggedProcessorMessages();
        assertThat(logs).anyMatch(m -> m.contains("Could not deserialize object from json"));
        assertThat(logs).anyMatch(m -> m.contains("Could not retrieve delay info from event"));
    }

    @Test
    void concurrentSubscribers_allReceiveSameDelayInfo() throws Exception {
        String train = "IC777";
        String station = "GYOR";
        DelayInfo info = delayInfo(train, station);
        String json = objectMapper.writeValueAsString(info);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<DelayInfo> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() ->
                    incomingMessageSink.getDelaySink().asFlux().take(1)
                            .subscribe(r -> {
                                results.add(r);
                                latch.countDown();
                            }));
        }

        Thread.sleep(100);

        testedObject.accept(message);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(results).hasSize(threads);
        results.forEach(r -> assertThat(r).usingRecursiveComparison().isEqualTo(info));
    }

    @Test
    void happyPath_noErrorLogs() throws Exception {
        String train = "IC888";
        String station = "DEBRECEN";
        DelayInfo info = delayInfo(train, station);
        String json = objectMapper.writeValueAsString(info);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);

        Message<Event<?, ?>> message = messageWithoutCorrelation(httpResponseEvent);

        testedObject.accept(message);

        assertThat(processorAppender.list)
                .noneMatch(e -> e.getLevel() == Level.ERROR);
    }
}
