package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.emma;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.helper.TimetableProcessingHelper;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveHttpEmmaRoutePlannerServiceTest {

    @Mock
    private EmmaTimetableService timetableService;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private StationService stationService;

    @Mock
    private WeatherService weatherService;

    @Mock
    private PredictionService predictionService;

    @Mock
    private TimetableProcessingHelper helper;

    @InjectMocks
    private ReactiveHttpEmmaRoutePlannerService testedObject;

    @Test
    void planRoute_fromBlank_returnsError_andNoDeps() {
        StepVerifier.create(testedObject.planRoute("  ", "TO", LocalDateTime.now(), null, null))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("from.empty", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(geocodingService, timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_toBlank_returnsError_andNoDeps() {
        StepVerifier.create(testedObject.planRoute("FROM", " ", LocalDateTime.now(), null, null))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("to.empty", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(geocodingService, timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_bothDatesNull_returnsError() {
        StepVerifier.create(testedObject.planRoute("FROM", "TO", null, null, null))
                .expectErrorSatisfies(ex -> {
                    assertInstanceOf(InvalidInputDataException.class, ex);
                    assertEquals("date.missing", ex.getMessage());
                })
                .verify();

        verifyNoInteractions(geocodingService, timetableService, stationService, weatherService, predictionService, helper);
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

        verifyNoInteractions(geocodingService, timetableService, stationService, weatherService, predictionService, helper);
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

        verifyNoInteractions(geocodingService, timetableService, stationService, weatherService, predictionService, helper);
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

        verifyNoInteractions(geocodingService, timetableService, stationService, weatherService, predictionService, helper);
    }

    @Test
    void planRoute_departureOnly_usesFilterByDeparture_andSkipsPredictionWhenTrainHasActuals() {
        String from = "FROM";
        String to = "TO";
        LocalDateTime departureTime = LocalDateTime.now().plusDays(1);
        LocalDate date = departureTime.toLocalDate();

        double fromLat = 47.5;
        double fromLon = 19.0;
        double toLat = 47.7;
        double toLon = 17.6;

        GeocodingResponse fromGeo = GeocodingResponse.builder()
                .latitude(fromLat)
                .longitude(fromLon)
                .address("FROM_ADDR")
                .build();
        GeocodingResponse toGeo = GeocodingResponse.builder()
                .latitude(toLat)
                .longitude(toLon)
                .address("TO_ADDR")
                .build();

        when(geocodingService.getCoordinates(from)).thenReturn(Mono.just(fromGeo));
        when(geocodingService.getCoordinates(to)).thenReturn(Mono.just(toGeo));

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

        when(timetableService.getTimetable(from, fromLat, fromLon, to, toLat, toLon, date))
                .thenReturn(Flux.just(timetableResponse));

        when(helper.filterByDeparture(eq(departureTime), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        StepVerifier.create(testedObject.planRoute(from, to, departureTime, null, null))
                .assertNext(route -> {
                    assertNotNull(route);
                    assertEquals(1, route.getTrains().size());
                    RouteResponse.Train t = route.getTrains().getFirst();
                    assertEquals("12345", t.getTrainNumber());
                    assertEquals("2025-01-01T08:05:00", t.getFromTimeActual());
                    assertEquals("2025-01-01T10:10:00", t.getToTimeActual());
                    assertNull(t.getFromTimePredicted());
                    assertNull(t.getToTimePredicted());
                })
                .verifyComplete();

        verify(geocodingService).getCoordinates(from);
        verify(geocodingService).getCoordinates(to);

        verify(timetableService).getTimetable(from, fromLat, fromLon, to, toLat, toLon, date);
        verify(helper).filterByDeparture(eq(departureTime), any());
        verify(helper, never()).filterByArrival(any(), any());
        verify(helper, never()).filterByChanges(anyInt(), any());

        verifyNoInteractions(stationService, weatherService, predictionService);
    }

    @Test
    void planRoute_arrivalOnly_usesFilterByArrival_andSkipsPredictionWhenTrainHasActuals() {
        String from = "FROM";
        String to = "TO";
        LocalDateTime arrivalTime = LocalDateTime.now().plusDays(1);
        LocalDate date = arrivalTime.toLocalDate();

        double fromLat = 47.5;
        double fromLon = 19.0;
        double toLat = 47.7;
        double toLon = 17.6;

        GeocodingResponse fromGeo = GeocodingResponse.builder()
                .latitude(fromLat)
                .longitude(fromLon)
                .address("FROM_ADDR")
                .build();
        GeocodingResponse toGeo = GeocodingResponse.builder()
                .latitude(toLat)
                .longitude(toLon)
                .address("TO_ADDR")
                .build();

        when(geocodingService.getCoordinates(from)).thenReturn(Mono.just(fromGeo));
        when(geocodingService.getCoordinates(to)).thenReturn(Mono.just(toGeo));

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

        when(timetableService.getTimetable(from, fromLat, fromLon, to, toLat, toLon, date))
                .thenReturn(Flux.just(timetableResponse));

        when(helper.filterByArrival(eq(arrivalTime), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        StepVerifier.create(testedObject.planRoute(from, to, null, arrivalTime, null))
                .assertNext(route -> {
                    assertNotNull(route);
                    assertEquals(1, route.getTrains().size());
                    RouteResponse.Train t = route.getTrains().getFirst();
                    assertEquals("12345", t.getTrainNumber());
                    assertEquals("2025-01-01T08:05:00", t.getFromTimeActual());
                    assertEquals("2025-01-01T10:10:00", t.getToTimeActual());
                    assertNull(t.getFromTimePredicted());
                    assertNull(t.getToTimePredicted());
                })
                .verifyComplete();

        verify(geocodingService).getCoordinates(from);
        verify(geocodingService).getCoordinates(to);

        verify(timetableService).getTimetable(from, fromLat, fromLon, to, toLat, toLon, date);
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

        double fromLat = 47.5;
        double fromLon = 19.0;
        double toLat = 47.7;
        double toLon = 17.6;

        GeocodingResponse fromGeo = GeocodingResponse.builder()
                .latitude(fromLat)
                .longitude(fromLon)
                .address("FROM_ADDR")
                .build();
        GeocodingResponse toGeo = GeocodingResponse.builder()
                .latitude(toLat)
                .longitude(toLon)
                .address("TO_ADDR")
                .build();

        when(geocodingService.getCoordinates(from)).thenReturn(Mono.just(fromGeo));
        when(geocodingService.getCoordinates(to)).thenReturn(Mono.just(toGeo));

        String scheduledFrom = "2025-01-01T08:00:00";
        String scheduledTo = "2025-01-01T10:00:00";
        String fullTrainNumber = "IC 12345";

        hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.Train train =
                hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.Train.builder()
                        .trainNumber(fullTrainNumber)
                        .lineNumber("L1")
                        .fromStation("FS")
                        .toStation("TS")
                        .fromTimeScheduled(scheduledFrom)
                        .toTimeScheduled(scheduledTo)
                        .build();

        hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse timetableResponse =
                hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.builder()
                        .trains(List.of(train))
                        .build();

        when(timetableService.getTimetable(from, fromLat, fromLon, to, toLat, toLon, date))
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
                .assertNext(route -> {
                    assertNotNull(route);
                    assertEquals(1, route.getTrains().size());
                    RouteResponse.Train t = route.getTrains().getFirst();

                    assertEquals(fullTrainNumber, t.getTrainNumber());
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

        verify(geocodingService).getCoordinates(from);
        verify(geocodingService).getCoordinates(to);
        verify(timetableService).getTimetable(from, fromLat, fromLon, to, toLat, toLon, date);
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

        assertEquals("12345", depCaptor.getValue().getTrainNumber());
        assertEquals("12345", arrCaptor.getValue().getTrainNumber());
        assertEquals("START", depCaptor.getValue().getStationCode());
        assertEquals("END", arrCaptor.getValue().getStationCode());
    }

    @Test
    void planRoute_routeNotFound_fallsBackWithoutWeatherOrPrediction() {
        String from = "FROM";
        String to = "TO";
        LocalDateTime departureTime = LocalDateTime.now().plusDays(1);
        LocalDate date = departureTime.toLocalDate();

        double fromLat = 47.5;
        double fromLon = 19.0;
        double toLat = 47.7;
        double toLon = 17.6;

        GeocodingResponse fromGeo = GeocodingResponse.builder()
                .latitude(fromLat)
                .longitude(fromLon)
                .address("FROM_ADDR")
                .build();
        GeocodingResponse toGeo = GeocodingResponse.builder()
                .latitude(toLat)
                .longitude(toLon)
                .address("TO_ADDR")
                .build();

        when(geocodingService.getCoordinates(from)).thenReturn(Mono.just(fromGeo));
        when(geocodingService.getCoordinates(to)).thenReturn(Mono.just(toGeo));

        String scheduledFrom = "2025-01-01T08:00:00";
        String scheduledTo = "2025-01-01T10:00:00";

        hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.Train train =
                hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.Train.builder()
                        .trainNumber("12345")
                        .lineNumber("L1")
                        .fromStation("FS")
                        .toStation("TS")
                        .fromTimeScheduled(scheduledFrom)
                        .toTimeScheduled(scheduledTo)
                        .build();

        hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse timetableResponse =
                hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.builder()
                        .trains(List.of(train))
                        .build();

        when(timetableService.getTimetable(from, fromLat, fromLon, to, toLat, toLon, date))
                .thenReturn(Flux.just(timetableResponse));

        when(helper.filterByDeparture(eq(departureTime), any()))
                .thenAnswer(i -> i.getArgument(1));

        when(stationService.getRoute("12345")).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.planRoute(from, to, departureTime, null, null))
                .assertNext(route -> {
                    assertNotNull(route);
                    assertEquals(1, route.getTrains().size());
                    RouteResponse.Train t = route.getTrains().getFirst();

                    assertEquals("12345", t.getTrainNumber());
                    assertEquals(scheduledFrom, t.getFromTimeScheduled());
                    assertEquals(scheduledTo, t.getToTimeScheduled());
                    assertNull(t.getFromTimePredicted());
                    assertNull(t.getToTimePredicted());
                    assertNull(t.getFromTimeActual());
                    assertNull(t.getToTimeActual());
                })
                .verifyComplete();

        verify(geocodingService).getCoordinates(from);
        verify(geocodingService).getCoordinates(to);
        verify(timetableService).getTimetable(from, fromLat, fromLon, to, toLat, toLon, date);
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

        double fromLat = 47.5;
        double fromLon = 19.0;
        double toLat = 47.7;
        double toLon = 17.6;

        GeocodingResponse fromGeo = GeocodingResponse.builder()
                .latitude(fromLat)
                .longitude(fromLon)
                .address("FROM_ADDR")
                .build();
        GeocodingResponse toGeo = GeocodingResponse.builder()
                .latitude(toLat)
                .longitude(toLon)
                .address("TO_ADDR")
                .build();

        when(geocodingService.getCoordinates(from)).thenReturn(Mono.just(fromGeo));
        when(geocodingService.getCoordinates(to)).thenReturn(Mono.just(toGeo));

        String scheduledFrom = "2025-01-01T08:00:00";
        String scheduledTo = "2025-01-01T10:00:00";

        hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.Train train =
                hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.Train.builder()
                        .trainNumber("12345")
                        .lineNumber("L1")
                        .fromStation("FS")
                        .toStation("TS")
                        .fromTimeScheduled(scheduledFrom)
                        .toTimeScheduled(scheduledTo)
                        .build();

        hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse timetableResponse =
                hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse.builder()
                        .trains(List.of(train))
                        .build();

        when(timetableService.getTimetable(from, fromLat, fromLon, to, toLat, toLon, date))
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
                .assertNext(route -> {
                    assertNotNull(route);
                    assertEquals(1, route.getTrains().size());
                    RouteResponse.Train t = route.getTrains().getFirst();

                    assertEquals("12345", t.getTrainNumber());
                    assertEquals(scheduledFrom, t.getFromTimeScheduled());
                    assertEquals(scheduledTo, t.getToTimeScheduled());
                    assertNull(t.getFromTimePredicted());
                    assertNull(t.getToTimePredicted());
                    assertNull(t.getFromTimeActual());
                    assertNull(t.getToTimeActual());
                })
                .verifyComplete();

        verify(geocodingService).getCoordinates(from);
        verify(geocodingService).getCoordinates(to);
        verify(timetableService).getTimetable(from, fromLat, fromLon, to, toLat, toLon, date);
        verify(helper).filterByDeparture(eq(departureTime), any());
        verify(stationService).getRoute("12345");
        verify(stationService).getStation("START");
        verify(stationService).getStation("END");

        verifyNoInteractions(weatherService, predictionService);
    }
}
