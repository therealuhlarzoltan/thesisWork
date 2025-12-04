package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.registry.WeatherInfoRegistryImpl;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherInfoProcessorTest {

    private ObjectMapper objectMapper;
    private IncomingMessageSink incomingMessageSink;
    private WeatherInfoRegistryImpl registry;

    @Mock
    private WeatherInfoCache weatherInfoCache;
    @Mock
    private HttpResponseEvent httpResponseEvent;
    @Mock
    private ResponsePayload responsePayload;

    private WeatherInfoProcessorImpl testedObject;

    private Logger processorLogger;
    private Logger registryLogger;
    private ListAppender<ILoggingEvent> processorAppender;
    private ListAppender<ILoggingEvent> registryAppender;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        incomingMessageSink = new IncomingMessageSink();
        registry = new WeatherInfoRegistryImpl(weatherInfoCache);
        ReflectionTestUtils.setField(registry, "timeout", 10);

        testedObject = new WeatherInfoProcessorImpl(objectMapper, registry, incomingMessageSink);

        processorLogger = (Logger) LoggerFactory.getLogger(WeatherInfoProcessorImpl.class);
        processorAppender = new ListAppender<>();
        processorAppender.start();
        processorLogger.addAppender(processorAppender);

        registryLogger = (Logger) LoggerFactory.getLogger(WeatherInfoRegistryImpl.class);
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

    private List<String> processorLogs() {
        return processorAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private List<String> registryLogs() {
        return registryAppender.list.stream()
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

    private Message buildMessage(Event<?, ?> payload) {
        return MessageBuilder.withPayload(payload).build();
    }

    @Test
    void accept_success_emitsToSinkCompletesRegistryAndCaches() throws Exception {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        WeatherInfo info = weather(station, time, 12.5);
        String json = objectMapper.writeValueAsString(info);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);
        when(weatherInfoCache.cacheWeatherInfo(info)).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = buildMessage(httpResponseEvent);

        final WeatherInfo[] registryResult = new WeatherInfo[1];
        CountDownLatch latch = new CountDownLatch(1);
        registry.waitForWeather(station, time)
                .subscribe(r -> {
                    registryResult[0] = r;
                    latch.countDown();
                });

        StepVerifier.create(incomingMessageSink.getWeatherSink().asFlux().take(1))
                .then(() -> testedObject.accept(message))
                .assertNext(resp -> assertThat(resp).usingRecursiveComparison().isEqualTo(info))
                .verifyComplete();

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(registryResult[0]).usingRecursiveComparison().isEqualTo(info);

        verify(weatherInfoCache).cacheWeatherInfo(info);

        List<String> pLogs = processorLogs();
        List<String> rLogs = registryLogs();
        assertThat(pLogs).anyMatch(m -> m.contains("Processing message created at"));
        assertThat(rLogs).anyMatch(m -> m.contains("Received weather info with key " + station + ":" + time));
    }

    @Test
    void accept_success_withNullTemperature_doesNotCache() throws Exception {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 15, 0);
        WeatherInfo info = weather(station, time, null);
        String json = objectMapper.writeValueAsString(info);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);

        Message<Event<?, ?>> message = buildMessage(httpResponseEvent);

        StepVerifier.create(incomingMessageSink.getWeatherSink().asFlux().take(1))
                .then(() -> testedObject.accept(message))
                .assertNext(resp -> assertThat(resp).usingRecursiveComparison().isEqualTo(info))
                .verifyComplete();

        verify(weatherInfoCache, never()).cacheWeatherInfo(any());
    }

    @Test
    void accept_error_logsErrorMessage_noSinkNoRegistry() {
        String rawError = "some-error";

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.ERROR);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(rawError);

        Message<Event<?, ?>> message = buildMessage(httpResponseEvent);

        testedObject.accept(message);

        StepVerifier.create(incomingMessageSink.getWeatherSink().asFlux().take(1))
                .expectTimeout(Duration.ofMillis(200))
                .verify();

        List<String> pLogs = processorLogs();
        assertThat(pLogs).anyMatch(m -> m.contains("Received an error response: " + rawError));
    }

    @Test
    void accept_unknownType_logsUnknown() {
        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(null);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn("{}");

        Message<Event<?, ?>> message = buildMessage(httpResponseEvent);

        testedObject.accept(message);

        List<String> pLogs = processorLogs();
        assertThat(pLogs).anyMatch(m -> m.contains("Received unknown event type"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void accept_whenPayloadNotHttpResponseEvent_logsUnexpectedAndReturns() {
        Event<String, String> genericEvent = mock(Event.class);

        Message<Event<?, ?>> message = buildMessage(genericEvent);

        testedObject.accept(message);

        List<String> pLogs = processorLogs();
        assertThat(pLogs).anyMatch(m -> m.contains("Unexpected event parameters, expected a HttpResponseEvent"));
        assertThat(pLogs).anyMatch(m -> m.contains("Could not retrieve response event from message"));
    }

    @Test
    void accept_whenWeatherInfoDeserializationFails_logsErrorAndDoesNotEmit() {
        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn("{not-valid-json");

        Message<Event<?, ?>> message = buildMessage(httpResponseEvent);

        testedObject.accept(message);

        StepVerifier.create(incomingMessageSink.getWeatherSink().asFlux().take(1))
                .expectTimeout(Duration.ofMillis(200))
                .verify();

        List<String> pLogs = processorLogs();
        List<String> rLogs = registryLogs();
        assertThat(pLogs).anyMatch(m -> m.contains("Could not deserialize object from json"));
        assertThat(rLogs).noneMatch(m -> m.contains("Received weather info"));
    }

    @Test
    void concurrentWaiters_allReceiveSameResponse_registryAndCacheCorrect() throws Exception {
        String station = "BPK";
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 18, 0);
        WeatherInfo info = weather(station, time, 11.0);
        String json = objectMapper.writeValueAsString(info);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);
        when(weatherInfoCache.cacheWeatherInfo(info)).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = buildMessage(httpResponseEvent);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<WeatherInfo> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() ->
                    registry.waitForWeather(station, time)
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

        verify(weatherInfoCache, times(1)).cacheWeatherInfo(info);
    }

    @Test
    void happyPath_noErrorLogs() throws Exception {
        String station = "BPK2";
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 10, 0);
        WeatherInfo info = weather(station, time, 9.5);
        String json = objectMapper.writeValueAsString(info);

        when(httpResponseEvent.getEventCreatedAt()).thenReturn(ZonedDateTime.now());
        when(httpResponseEvent.getEventType()).thenReturn(HttpResponseEvent.Type.SUCCESS);
        when(httpResponseEvent.getData()).thenReturn(responsePayload);
        when(responsePayload.getMessage()).thenReturn(json);
        when(weatherInfoCache.cacheWeatherInfo(info)).thenReturn(Mono.empty());

        Message<Event<?, ?>> message = buildMessage(httpResponseEvent);

        testedObject.accept(message);

        assertThat(processorAppender.list)
                .noneMatch(e -> e.getLevel() == Level.ERROR);
    }
}
