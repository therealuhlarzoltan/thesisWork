package hu.uni_obuda.thesis.railways.data.raildatacollector.worker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.TimetableCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CacheEvictorTest {

    @Mock
    private TimetableCache timetableCache;

    private CacheEvictor cacheEvictor;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setUp() {
        cacheEvictor = new CacheEvictor(timetableCache);
        Logger logger = (Logger) LoggerFactory.getLogger(CacheEvictor.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(CacheEvictor.class);
        logger.detachAppender(listAppender);
    }

    @Test
    public void evictCache_evictionSucceeds_infoLogsWritten() {
        when(timetableCache.evictAll()).thenReturn(Mono.empty());

        cacheEvictor.evictCache();

        verify(timetableCache, times(1)).evictAll();
        assertTrue(listAppender.list.size() >= 2);

        ILoggingEvent firstLog = listAppender.list.get(0);
        ILoggingEvent secondLog = listAppender.list.get(1);

        assertEquals(Level.INFO, firstLog.getLevel());
        assertTrue(firstLog.getFormattedMessage().contains("Evicting timetable cache"));

        assertEquals(Level.INFO, secondLog.getLevel());
        assertTrue(secondLog.getFormattedMessage().contains("Timetable cache eviction completed"));
    }

    @Test
    public void evictCache_evictionFails_errorLogWritten() {
        when(timetableCache.evictAll()).thenReturn(Mono.error(new RuntimeException("eviction failed")));

        cacheEvictor.evictCache();

        verify(timetableCache, times(1)).evictAll();
        assertTrue(listAppender.list.size() >= 2);

        ILoggingEvent firstLog = listAppender.list.get(0);
        ILoggingEvent secondLog = listAppender.list.get(1);

        assertEquals(Level.INFO, firstLog.getLevel());
        assertTrue(firstLog.getFormattedMessage().contains("Evicting timetable cache"));

        assertEquals(Level.ERROR, secondLog.getLevel());
        assertTrue(secondLog.getFormattedMessage().contains("Failed to evict timetable cache"));
    }
}
