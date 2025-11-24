package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.scheduled;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainRouteEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainStationEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainRouteRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainStationRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.DelayFetcherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalDebugTrainDelayProcessorTest {

    @Mock
    private TrainRouteRepository trainRouteRepository;
    @Mock
    private DelayFetcherService delayFetcherService;
    @Mock
    private TrainStatusCache trainStatusCache;
    @Mock
    private TrainStationRepository trainStationRepository;

    private Scheduler scheduler;

    private LocalDebugTrainDelayProcessorImpl testedObject;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        scheduler = Schedulers.immediate();
        testedObject = new LocalDebugTrainDelayProcessorImpl(
                scheduler,
                trainRouteRepository,
                delayFetcherService,
                trainStatusCache,
                trainStationRepository
        );

        logger = (Logger) LoggerFactory.getLogger(LocalDebugTrainDelayProcessorImpl.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        if (logger != null && appender != null) {
            logger.detachAppender(appender);
        }
    }

    private List<String> logs() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    void processTrainRoutes_logsStartupAndWarning_only() {
        testedObject.processTrainRoutes();

        List<String> logs = logs();
        assertThat(logs).anyMatch(m -> m.contains("Data fetch started..."));
        assertThat(logs).anyMatch(m -> m.contains("Local debug mode is active, no data will be fetched automatically!"));

        verifyNoInteractions(trainRouteRepository, delayFetcherService, trainStatusCache, trainStationRepository);
    }

    @Test
    void processTrainRoute_whenRouteNotFound_logsAndDoesNothing() {
        String trainNumber = "IC123";
        when(trainRouteRepository.findById(trainNumber)).thenReturn(Mono.empty());

        testedObject.processTrainRoute(trainNumber);

        verify(trainRouteRepository).findById(trainNumber);
        verifyNoInteractions(delayFetcherService, trainStatusCache, trainStationRepository);

        List<String> logs = logs();
        assertThat(logs).anyMatch(m -> m.contains("Data fetch started for single train with train number: " + trainNumber));
    }

    @Test
    void processTrainRoute_whenAlreadyComplete_doesNotFetch() {
        String trainNumber = "IC456";

        TrainRouteEntity route = mock(TrainRouteEntity.class);
        when(route.getTrainNumber()).thenReturn(trainNumber);
        when(trainRouteRepository.findById(trainNumber)).thenReturn(Mono.just(route));
        when(trainStatusCache.isComplete(eq(trainNumber), any(LocalDate.class))).thenReturn(Mono.just(true));

        testedObject.processTrainRoute(trainNumber);

        verify(trainRouteRepository).findById(trainNumber);
        verify(trainStatusCache).isComplete(eq(trainNumber), any(LocalDate.class));
        verifyNoInteractions(trainStationRepository, delayFetcherService);

        List<String> logs = logs();
        assertThat(logs).anyMatch(m -> m.contains("Fetching delay for train number " + trainNumber));
        assertThat(logs).anyMatch(m -> m.contains("Data for train number " + trainNumber + " is already present for today"));
    }

    @Test
    void processTrainRoute_whenIncompleteAndMissingStartCoords_doesNotFetch() {
        String trainNumber = "IC789";

        TrainRouteEntity route = mock(TrainRouteEntity.class);
        when(route.getTrainNumber()).thenReturn(trainNumber);
        when(route.getFrom()).thenReturn("START");
        when(route.getTo()).thenReturn("END");

        TrainStationEntity startStation = mock(TrainStationEntity.class);
        TrainStationEntity endStation = mock(TrainStationEntity.class);
        when(startStation.getLatitude()).thenReturn(null);
        when(startStation.getLongitude()).thenReturn(19.1);
        when(endStation.getLatitude()).thenReturn(47.0);
        when(endStation.getLongitude()).thenReturn(19.0);

        when(trainRouteRepository.findById(trainNumber)).thenReturn(Mono.just(route));
        when(trainStatusCache.isComplete(eq(trainNumber), any(LocalDate.class))).thenReturn(Mono.just(false));
        when(trainStationRepository.findByStationCode("START")).thenReturn(Mono.just(startStation));
        when(trainStationRepository.findByStationCode("END")).thenReturn(Mono.just(endStation));

        testedObject.processTrainRoute(trainNumber);

        verify(trainRouteRepository).findById(trainNumber);
        verify(trainStatusCache).isComplete(eq(trainNumber), any(LocalDate.class));
        verify(trainStationRepository).findByStationCode("START");
        verify(trainStationRepository).findByStationCode("END");
        verifyNoInteractions(delayFetcherService);
    }

    @Test
    void processTrainRoute_whenIncompleteAndMissingEndCoords_doesNotFetch() {
        String trainNumber = "IC999";

        TrainRouteEntity route = mock(TrainRouteEntity.class);
        when(route.getTrainNumber()).thenReturn(trainNumber);
        when(route.getFrom()).thenReturn("START");
        when(route.getTo()).thenReturn("END");

        TrainStationEntity startStation = mock(TrainStationEntity.class);
        TrainStationEntity endStation = mock(TrainStationEntity.class);
        when(startStation.getLatitude()).thenReturn(47.5);
        when(startStation.getLongitude()).thenReturn(19.1);
        when(endStation.getLatitude()).thenReturn(null);
        when(endStation.getLongitude()).thenReturn(19.0);

        when(trainRouteRepository.findById(trainNumber)).thenReturn(Mono.just(route));
        when(trainStatusCache.isComplete(eq(trainNumber), any(LocalDate.class))).thenReturn(Mono.just(false));
        when(trainStationRepository.findByStationCode("START")).thenReturn(Mono.just(startStation));
        when(trainStationRepository.findByStationCode("END")).thenReturn(Mono.just(endStation));

        testedObject.processTrainRoute(trainNumber);

        verify(trainRouteRepository).findById(trainNumber);
        verify(trainStatusCache).isComplete(eq(trainNumber), any(LocalDate.class));
        verify(trainStationRepository).findByStationCode("START");
        verify(trainStationRepository).findByStationCode("END");
        verifyNoInteractions(delayFetcherService);
    }

    @Test
    void processTrainRoute_whenIncompleteAndCoordsPresent_fetchesDelayWithAdjustedStationCodes() {
        String trainNumber = "IC111";
        String fromRaw = "GYŐR";
        String fromAdjusted = "GYÕR";
        String toRaw = "BUDAPEST-KELETI";
        String toAdjusted = toRaw;

        TrainRouteEntity route = mock(TrainRouteEntity.class);
        when(route.getTrainNumber()).thenReturn(trainNumber);
        when(route.getFrom()).thenReturn(fromRaw);
        when(route.getTo()).thenReturn(toRaw);

        TrainStationEntity startStation = mock(TrainStationEntity.class);
        TrainStationEntity endStation = mock(TrainStationEntity.class);
        when(startStation.getLatitude()).thenReturn(47.68);
        when(startStation.getLongitude()).thenReturn(17.64);
        when(endStation.getLatitude()).thenReturn(47.5);
        when(endStation.getLongitude()).thenReturn(19.08);

        when(trainRouteRepository.findById(trainNumber)).thenReturn(Mono.just(route));
        when(trainStatusCache.isComplete(eq(trainNumber), any(LocalDate.class))).thenReturn(Mono.just(false));
        when(trainStationRepository.findByStationCode(fromAdjusted)).thenReturn(Mono.just(startStation));
        when(trainStationRepository.findByStationCode(toAdjusted)).thenReturn(Mono.just(endStation));

        testedObject.processTrainRoute(trainNumber);

        verify(trainRouteRepository).findById(trainNumber);
        verify(trainStatusCache).isComplete(eq(trainNumber), any(LocalDate.class));
        verify(trainStationRepository).findByStationCode(fromAdjusted);
        verify(trainStationRepository).findByStationCode(toAdjusted);

        ArgumentCaptor<String> fromCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> fromLatCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> fromLonCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> toLatCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> toLonCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);

        verify(delayFetcherService).fetchDelay(
                eq(trainNumber),
                fromCaptor.capture(),
                fromLatCaptor.capture(),
                fromLonCaptor.capture(),
                toCaptor.capture(),
                toLatCaptor.capture(),
                toLonCaptor.capture(),
                dateCaptor.capture()
        );

        assertThat(fromCaptor.getValue()).isEqualTo(fromRaw);
        assertThat(toCaptor.getValue()).isEqualTo(toRaw);
        assertThat(fromLatCaptor.getValue()).isEqualTo(47.68);
        assertThat(fromLonCaptor.getValue()).isEqualTo(17.64);
        assertThat(toLatCaptor.getValue()).isEqualTo(47.5);
        assertThat(toLonCaptor.getValue()).isEqualTo(19.08);
        assertThat(dateCaptor.getValue()).isNotNull();

        List<String> logs = logs();
        assertThat(logs).anyMatch(m -> m.contains("Fetching delay for train number " + trainNumber));
        assertThat(logs).anyMatch(m -> m.contains("Calling fetcher service for train number " + trainNumber));
    }

    @Test
    void resolveOperationalDate_before3am_returnsPreviousDay() {
        LocalDateTime dt = LocalDateTime.of(2025, 1, 2, 2, 30);
        LocalDate result = ReflectionTestUtils.invokeMethod(
                testedObject,
                "resolveOperationalDate",
                dt
        );
        assertThat(result).isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    void resolveOperationalDate_after3am_returnsSameDay() {
        LocalDateTime dt = LocalDateTime.of(2025, 1, 2, 10, 0);
        LocalDate result = ReflectionTestUtils.invokeMethod(
                testedObject,
                "resolveOperationalDate",
                dt
        );
        assertThat(result).isEqualTo(LocalDate.of(2025, 1, 2));
    }

    @Test
    void adjustStationCodeFormat_replacesHungarianCharacters() {
        String original = "GYŐR-ÚJHEGY";
        String result = ReflectionTestUtils.invokeMethod(
                testedObject,
                "adjustStationCodeFormat",
                original
        );
        assertThat(result).isEqualTo("GYÕR-ÛJHEGY");
    }
}
