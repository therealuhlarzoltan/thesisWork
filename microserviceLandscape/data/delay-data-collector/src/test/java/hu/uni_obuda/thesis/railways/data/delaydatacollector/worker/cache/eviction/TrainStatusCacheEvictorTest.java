package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainStatusCacheEvictorTest {

    @Mock
    private TrainStatusCache trainStatusCache;

    @InjectMocks
    private TrainStatusCacheEvictor testedObject;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(TrainStatusCacheEvictor.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    private List<String> loggedMessages() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    void evict_whenEvictionSucceeds_logsStartAndCompletion() {
        when(trainStatusCache.evictAll()).thenReturn(Mono.empty());

        testedObject.evict();

        verify(trainStatusCache).evictAll();

        List<ILoggingEvent> events = listAppender.list;
        List<String> messages = loggedMessages();

        assertThat(messages)
                .anyMatch(msg -> msg.contains("Evicting train status cache..."));

        assertThat(messages)
                .anyMatch(msg -> msg.contains("TrainStatus cache eviction completed."));

        assertThat(events)
                .noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    void evict_whenEvictionFails_logsStartAndError() {
        RuntimeException ex = new RuntimeException("boom-train-status");
        when(trainStatusCache.evictAll()).thenReturn(Mono.error(ex));

        testedObject.evict();

        verify(trainStatusCache).evictAll();

        List<ILoggingEvent> events = listAppender.list;
        List<String> messages = loggedMessages();

        assertThat(messages)
                .anyMatch(msg -> msg.contains("Evicting train status cache..."));

        ILoggingEvent errorEvent = events.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No ERROR log found"));

        assertThat(errorEvent.getFormattedMessage())
                .contains("Failed to evict train status cache");

        assertThat(errorEvent.getThrowableProxy()).isNotNull();
        assertThat(errorEvent.getThrowableProxy().getMessage())
                .contains("boom-train-status");
    }
}
