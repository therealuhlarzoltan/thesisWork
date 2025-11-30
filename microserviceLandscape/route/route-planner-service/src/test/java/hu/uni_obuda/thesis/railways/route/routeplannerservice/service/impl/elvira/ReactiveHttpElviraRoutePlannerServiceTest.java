package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.elvira;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.helper.TimetableProcessingHelper;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.ElviraTimetableService;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.PredictionService;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.StationService;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.WeatherService;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.github.benmanes.caffeine.cache.Cache;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveHttpElviraRoutePlannerServiceTest {

    @Mock
    private ElviraTimetableService timetableService;
    @Mock
    private Cache<String, List<TrainRouteResponse>> routeCache;
    @Mock
    private StationService stationService;
    @Mock
    private WeatherService weatherService;
    @Mock
    private PredictionService predictionService;
    @Mock
    private TimetableProcessingHelper helper;

    @InjectMocks
    private ReactiveHttpElviraRoutePlannerService testedObject;

    @Test
    void planRoute_fromBlank_returnsError_andNoDeps() {
        StepVerifier.create(testedObject.planRoute("  ", "TO", LocalDateTime.now(), null, null))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("from.empty", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_toBlank_returnsError_andNoDeps() {
        StepVerifier.create(testedObject.planRoute("FROM", " ", LocalDateTime.now(), null, null))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("to.empty", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_bothDatesNull_returnsError() {
        StepVerifier.create(testedObject.planRoute("FROM", "TO", null, null, null))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("date.missing", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_arrivalBeforeNow_returnsError() {
        LocalDateTime arrival = LocalDateTime.now().minusHours(1);

        StepVerifier.create(testedObject.planRoute("FROM", "TO", null, arrival, null))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("arrival.date.before.now", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_arrivalBeforeDeparture_returnsError() {
        LocalDateTime nowPlus = LocalDateTime.now().plusDays(1);
        LocalDateTime departure = nowPlus.plusHours(2);
        LocalDateTime arrival = nowPlus;

        StepVerifier.create(testedObject.planRoute("FROM", "TO", departure, arrival, null))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("arrival.date.before.departure", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_negativeMaxChanges_returnsError() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);

        StepVerifier.create(testedObject.planRoute("FROM", "TO", departure, null, -1))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("changes.negative", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_departureOnly_usesFilterByDeparture_andSkipsStationsWhenTrainHasActuals() {
        String from = "FROM";
        String to = "TO";
        LocalDateTime departureTime = LocalDateTime.now().plusDays(1);
        LocalDate date = departureTime.toLocalDate();

        TrainRouteResponse.Train train =
                TrainRouteResponse.Train.builder()
                        .trainNumber("12345")
                        .lineNumber("L1")
                        .fromStation("FS")
                        .toStation("TS")
                        .fromTimeScheduled("2025-01-01T08:00:00")
                        .toTimeScheduled("2025-01-01T10:00:00")
                        .fromTimeActual("2025-01-01T08:05:00")
                        .toTimeActual("2025-01-01T10:10:00")
                        .build();

        TrainRouteResponse timetableResponse =
                TrainRouteResponse.builder()
                        .trains(List.of(train))
                        .build();

        when(timetableService.getTimetable(anyString(), anyString(), eq(date)))
                .thenReturn(Flux.just(timetableResponse));

        when(helper.filterByDeparture(eq(departureTime), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        StepVerifier.create(testedObject.planRoute(from, to, departureTime, null, null))
                .assertNext(routeResponse -> {
                    assertNotNull(routeResponse);
                    assertEquals(1, routeResponse.getTrains().size());
                    RouteResponse.Train t = routeResponse.getTrains().getFirst();
                    assertEquals("12345", t.getTrainNumber());
                    assertEquals("2025-01-01T08:05:00", t.getFromTimeActual());
                    assertEquals("2025-01-01T10:10:00", t.getToTimeActual());
                    assertNull(t.getFromTimePredicted());
                    assertNull(t.getToTimePredicted());
                })
                .verifyComplete();

        verify(timetableService).getTimetable(anyString(), anyString(), eq(date));
        verify(helper).filterByDeparture(eq(departureTime), any());
        verify(helper, never()).filterByArrival(any(), any());
        verify(helper, never()).filterByChanges(anyInt(), any());

        verifyNoInteractions(stationService, weatherService, predictionService);
    }

    @Test
    void planRoute_arrivalOnly_usesFilterByArrival() {
        String from = "FROM";
        String to = "TO";
        LocalDateTime arrivalTime = LocalDateTime.now().plusDays(1);
        LocalDate date = arrivalTime.toLocalDate();

        hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.Train train =
                hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.Train.builder()
                        .trainNumber("12345")
                        .lineNumber("L1")
                        .fromStation("FS")
                        .toStation("TS")
                        .fromTimeScheduled("2025-01-01T08:00:00")
                        .toTimeScheduled("2025-01-01T10:00:00")
                        .fromTimeActual("2025-01-01T08:05:00")
                        .toTimeActual("2025-01-01T10:10:00")
                        .build();

        hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse timetableResponse =
                hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.builder()
                        .trains(List.of(train))
                        .build();

        when(timetableService.getTimetable(anyString(), anyString(), eq(date)))
                .thenReturn(Flux.just(timetableResponse));

        when(helper.filterByArrival(eq(arrivalTime), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        StepVerifier.create(testedObject.planRoute(from, to, null, arrivalTime, null))
                .assertNext(routeResponse -> {
                    assertNotNull(routeResponse);
                    assertEquals(1, routeResponse.getTrains().size());
                    RouteResponse.Train result = routeResponse.getTrains().getFirst();

                    assertEquals("12345", result.getTrainNumber());
                    assertEquals("2025-01-01T08:05:00", result.getFromTimeActual());
                    assertEquals("2025-01-01T10:10:00", result.getToTimeActual());
                    assertNull(result.getFromTimePredicted());
                    assertNull(result.getToTimePredicted());
                })
                .verifyComplete();

        verify(timetableService).getTimetable(anyString(), anyString(), eq(date));
        verify(helper, never()).filterByDeparture(any(), any());
        verify(helper).filterByArrival(eq(arrivalTime), any());
        verify(helper, never()).filterByChanges(anyInt(), any());

        verifyNoInteractions(stationService, weatherService, predictionService);
    }

    @Test
    void planRoute_allFilters_noActuals_callsAllDeps_andBuildsPredictions() {
        String from = "FROM";
        String to = "TO";

        LocalDateTime departureTime = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime arrivalTime = departureTime.plusHours(2);
        int maxChanges = 1;
        LocalDate date = departureTime.toLocalDate();

        String scheduledFrom = "2025-01-01T08:00:00";
        String scheduledTo = "2025-01-01T10:00:00";

        TrainRouteResponse.Train train =
                TrainRouteResponse.Train.builder()
                        .trainNumber("12345")
                        .lineNumber("L1")
                        .fromStation("FS")
                        .toStation("TS")
                        .fromTimeScheduled(scheduledFrom)
                        .toTimeScheduled(scheduledTo)
                        .build();

        TrainRouteResponse timetableResponse =
                TrainRouteResponse.builder()
                        .trains(List.of(train))
                        .build();

        when(timetableService.getTimetable(anyString(), anyString(), eq(date)))
                .thenReturn(Flux.just(timetableResponse));

        when(helper.filterByDeparture(eq(departureTime), any())).thenAnswer(i -> i.getArgument(1));
        when(helper.filterByArrival(eq(arrivalTime), any())).thenAnswer(i -> i.getArgument(1));
        when(helper.filterByChanges(eq(maxChanges), any())).thenAnswer(i -> i.getArgument(1));

        hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse routeInfo =
                new hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse(
                        "12345", "L1", "START", "END"
                );
        when(stationService.getRoute("12345")).thenReturn(Mono.just(routeInfo));

        TrainStationResponse fromStation = TrainStationResponse.builder()
                .stationCode("START")
                .latitude(47.0)
                .longitude(19.0)
                .build();

        TrainStationResponse toStation = TrainStationResponse.builder()
                .stationCode("END")
                .latitude(48.0)
                .longitude(20.0)
                .build();

        when(stationService.getStation("START")).thenReturn(Mono.just(fromStation));
        when(stationService.getStation("END")).thenReturn(Mono.just(toStation));

        WeatherInfo fromWeather = WeatherInfo.builder().build();
        WeatherInfo toWeather = WeatherInfo.builder().build();

        when(weatherService.getWeather(eq("START"), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(Mono.just(fromWeather));
        when(weatherService.getWeather(eq("END"), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(Mono.just(toWeather));

        DelayPredictionResponse fromDelay = DelayPredictionResponse.builder()
                .trainNumber("12345")
                .stationCode("START")
                .predictedDelay(5.0)
                .build();
        DelayPredictionResponse toDelay = DelayPredictionResponse.builder()
                .trainNumber("12345")
                .stationCode("END")
                .predictedDelay(10.0)
                .build();

        when(predictionService.predictDepartureDelay(any(DelayPredictionRequest.class)))
                .thenReturn(Mono.just(fromDelay));
        when(predictionService.predictArrivalDelay(any(DelayPredictionRequest.class)))
                .thenReturn(Mono.just(toDelay));

        StepVerifier.create(testedObject.planRoute(from, to, departureTime, arrivalTime, maxChanges))
                .assertNext(routeResponse -> {
                    assertNotNull(routeResponse);
                    assertEquals(1, routeResponse.getTrains().size());
                    RouteResponse.Train t = routeResponse.getTrains().getFirst();

                    assertEquals("12345", t.getTrainNumber());
                    assertEquals(scheduledFrom, t.getFromTimeScheduled());
                    assertEquals(scheduledTo, t.getToTimeScheduled());

                    String expectedFromPred =
                            LocalDateTime.parse(scheduledFrom).plusMinutes(5).toString();
                    String expectedToPred =
                            LocalDateTime.parse(scheduledTo).plusMinutes(10).toString();

                    assertEquals(expectedFromPred, t.getFromTimePredicted());
                    assertEquals(expectedToPred, t.getToTimePredicted());
                })
                .verifyComplete();

        verify(timetableService).getTimetable(anyString(), anyString(), eq(date));
        verify(helper).filterByDeparture(eq(departureTime), any());
        verify(helper).filterByArrival(eq(arrivalTime), any());
        verify(helper).filterByChanges(eq(maxChanges), any());

        verify(stationService).getRoute("12345");
        verify(stationService).getStation("START");
        verify(stationService).getStation("END");

        verify(weatherService).getWeather(eq("START"), anyDouble(), anyDouble(), any(LocalDateTime.class));
        verify(weatherService).getWeather(eq("END"), anyDouble(), anyDouble(), any(LocalDateTime.class));

        ArgumentCaptor<DelayPredictionRequest> depCaptor = ArgumentCaptor.forClass(DelayPredictionRequest.class);
        ArgumentCaptor<DelayPredictionRequest> arrCaptor = ArgumentCaptor.forClass(DelayPredictionRequest.class);

        verify(predictionService).predictDepartureDelay(depCaptor.capture());
        verify(predictionService).predictArrivalDelay(arrCaptor.capture());

        assertEquals("START", depCaptor.getValue().getStationCode());
        assertEquals("END", arrCaptor.getValue().getStationCode());
        assertEquals("12345", depCaptor.getValue().getTrainNumber());
        assertEquals("12345", arrCaptor.getValue().getTrainNumber());
    }

    @Test
    void planRoute_routeNotFoundInStationService_fallsBackWithoutWeatherOrPrediction() {
        String from = "FROM";
        String to = "TO";
        LocalDateTime departureTime = LocalDateTime.now().plusDays(1);
        LocalDate date = departureTime.toLocalDate();

        String scheduledFrom = "2025-01-01T08:00:00";
        String scheduledTo = "2025-01-01T10:00:00";

        TrainRouteResponse.Train train =
                TrainRouteResponse.Train.builder()
                        .trainNumber("12345")
                        .lineNumber("L1")
                        .fromStation("FS")
                        .toStation("TS")
                        .fromTimeScheduled(scheduledFrom)
                        .toTimeScheduled(scheduledTo)
                        .build();

        TrainRouteResponse timetableResponse =
                TrainRouteResponse.builder()
                        .trains(List.of(train))
                        .build();

        when(timetableService.getTimetable(anyString(), anyString(), eq(date)))
                .thenReturn(Flux.just(timetableResponse));
        when(helper.filterByDeparture(eq(departureTime), any()))
                .thenAnswer(i -> i.getArgument(1));

        when(stationService.getRoute("12345")).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.planRoute(from, to, departureTime, null, null))
                .assertNext(routeResponse -> {
                    assertNotNull(routeResponse);
                    assertEquals(1, routeResponse.getTrains().size());
                    RouteResponse.Train t = routeResponse.getTrains().getFirst();

                    assertEquals("12345", t.getTrainNumber());
                    assertEquals(scheduledFrom, t.getFromTimeScheduled());
                    assertEquals(scheduledTo, t.getToTimeScheduled());
                    assertNull(t.getFromTimePredicted());
                    assertNull(t.getToTimePredicted());
                    assertNull(t.getFromTimeActual());
                    assertNull(t.getToTimeActual());
                })
                .verifyComplete();

        verify(timetableService).getTimetable(anyString(), anyString(), eq(date));
        verify(helper).filterByDeparture(eq(departureTime), any());
        verify(stationService).getRoute("12345");
        verify(stationService, never()).getStation(anyString());
        verifyNoInteractions(weatherService, predictionService);
    }

    @Test
    void planRoute_stationsMissingCoordinates_fallsBackWithoutWeatherOrPrediction() {
        String from = "FROM";
        String to = "TO";
        LocalDateTime departureTime = LocalDateTime.now().plusDays(1);
        LocalDate date = departureTime.toLocalDate();

        String scheduledFrom = "2025-01-01T08:00:00";
        String scheduledTo = "2025-01-01T10:00:00";

        TrainRouteResponse.Train train =
                TrainRouteResponse.Train.builder()
                        .trainNumber("12345")
                        .lineNumber("L1")
                        .fromStation("FS")
                        .toStation("TS")
                        .fromTimeScheduled(scheduledFrom)
                        .toTimeScheduled(scheduledTo)
                        .build();

        TrainRouteResponse timetableResponse =
                TrainRouteResponse.builder()
                        .trains(List.of(train))
                        .build();

        when(timetableService.getTimetable(anyString(), anyString(), eq(date)))
                .thenReturn(Flux.just(timetableResponse));
        when(helper.filterByDeparture(eq(departureTime), any()))
                .thenAnswer(i -> i.getArgument(1));

        hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse routeInfo =
                new hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse(
                        "12345", "L1", "START", "END"
                );
        when(stationService.getRoute("12345")).thenReturn(Mono.just(routeInfo));

        TrainStationResponse fromStation = TrainStationResponse.builder()
                .stationCode("START")
                .latitude(null)
                .longitude(19.0)
                .build();
        TrainStationResponse toStation = TrainStationResponse.builder()
                .stationCode("END")
                .latitude(48.0)
                .longitude(20.0)
                .build();

        when(stationService.getStation("START")).thenReturn(Mono.just(fromStation));
        when(stationService.getStation("END")).thenReturn(Mono.just(toStation));

        StepVerifier.create(testedObject.planRoute(from, to, departureTime, null, null))
                .assertNext(routeResponse -> {
                    assertNotNull(routeResponse);
                    assertEquals(1, routeResponse.getTrains().size());
                    RouteResponse.Train t = routeResponse.getTrains().getFirst();

                    assertEquals("12345", t.getTrainNumber());
                    assertEquals(scheduledFrom, t.getFromTimeScheduled());
                    assertEquals(scheduledTo, t.getToTimeScheduled());
                    assertNull(t.getFromTimePredicted());
                    assertNull(t.getToTimePredicted());
                    assertNull(t.getFromTimeActual());
                    assertNull(t.getToTimeActual());
                })
                .verifyComplete();

        verify(timetableService).getTimetable(anyString(), anyString(), eq(date));
        verify(helper).filterByDeparture(eq(departureTime), any());
        verify(stationService).getRoute("12345");
        verify(stationService).getStation("START");
        verify(stationService).getStation("END");
        verifyNoInteractions(weatherService, predictionService);
    }
}
