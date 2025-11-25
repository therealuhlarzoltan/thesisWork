package hu.uni_obuda.thesis.railways.data.raildatacollector.service.data.impl;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway.EmmaRailDataGateway;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.EmmaTimetableCache;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data.EmmaDelayMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data.EmmaRouteMapper;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.TrainNotInServiceException;
import io.netty.channel.ConnectTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmmaRailDataServiceTest {

    @Mock
    private EmmaRailDataGateway gateway;
    @Mock
    private EmmaTimetableCache cache;
    @Mock
    private EmmaDelayMapper delayMapper;
    @Mock
    private EmmaRouteMapper routeMapper;

    @InjectMocks
    private EmmaRailDataServiceImpl testedObject;

    private static long toEpochMillis(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private EmmaShortTimetableResponse buildSingleLegTimetable(LocalDate date, String trainNumber, String gtfsId) {
        EmmaShortTimetableResponse response = new EmmaShortTimetableResponse();
        EmmaShortTimetableResponse.Plan plan = new EmmaShortTimetableResponse.Plan();
        EmmaShortTimetableResponse.Itinerary itinerary = new EmmaShortTimetableResponse.Itinerary();
        EmmaShortTimetableResponse.Leg leg = new EmmaShortTimetableResponse.Leg();
        EmmaShortTimetableResponse.Route route = new EmmaShortTimetableResponse.Route();
        EmmaShortTimetableResponse.Trip trip = new EmmaShortTimetableResponse.Trip();

        route.setLongName("SomeRoute");
        trip.setTripShortName(trainNumber + " something");
        trip.setGtfsId(gtfsId);

        leg.setRoute(route);
        leg.setTrip(trip);
        leg.setMode("RAIL");
        long start = toEpochMillis(date, 1, 0);
        long end = toEpochMillis(date, 2, 0);
        leg.setStartTime(start);
        leg.setEndTime(end);

        itinerary.setLegs(List.of(leg));
        plan.setItineraries(List.of(itinerary));
        response.setPlan(plan);
        return response;
    }

    @Test
    void getDelayInfo_cacheHit_returnsDelayInfo() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        EmmaShortTimetableResponse resp = buildSingleLegTimetable(date, "123", "GTFS-123");

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(true));
        when(cache.get("A", "B", date)).thenReturn(Mono.just(resp));

        EmmaShortTrainDetailsResponse details = new EmmaShortTrainDetailsResponse();
        when(gateway.getShortTrainDetails("GTFS-123", date)).thenReturn(Mono.just(details));

        DelayInfo info = DelayInfo.builder().trainNumber("123").build();
        when(delayMapper.mapToDelayInfo(details, "123", date)).thenReturn(Mono.just(List.of(info)));

        StepVerifier.create(testedObject.getDelayInfo("123", "A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectNext(info)
                .verifyComplete();
    }

    @Test
    void getDelayInfo_cacheMiss_getsAndCachesTimetable() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        EmmaShortTimetableResponse resp = buildSingleLegTimetable(date, "456", "GTFS-456");

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", fromLat, fromLon, "B", toLat, toLon, date)).thenReturn(Mono.just(resp));
        when(cache.cache(eq("A"), eq("B"), eq(date), eq(resp))).thenReturn(Mono.empty());

        EmmaShortTrainDetailsResponse details = new EmmaShortTrainDetailsResponse();
        when(gateway.getShortTrainDetails("GTFS-456", date)).thenReturn(Mono.just(details));

        DelayInfo info = DelayInfo.builder().trainNumber("456").build();
        when(delayMapper.mapToDelayInfo(details, "456", date)).thenReturn(Mono.just(List.of(info)));

        StepVerifier.create(testedObject.getDelayInfo("456", "A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectNext(info)
                .verifyComplete();

        verify(cache).cache("A", "B", date, resp);
    }

    @Test
    void getDelayInfo_emptyTimetable_throwsFormatException() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        EmmaShortTimetableResponse resp = new EmmaShortTimetableResponse();
        EmmaShortTimetableResponse.Plan plan = new EmmaShortTimetableResponse.Plan();
        resp.setPlan(plan);

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", fromLat, fromLon, "B", toLat, toLon, date))
                .thenReturn(Mono.just(resp));

        StepVerifier.create(testedObject.getDelayInfo("999", "A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectError(ExternalApiFormatMismatchException.class)
                .verify();
    }

    @Test
    void getDelayInfo_trainNotFound_throwsTrainNotInService() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        EmmaShortTimetableResponse resp = buildSingleLegTimetable(date, "AAA", "GTFS-AAA");

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", fromLat, fromLon, "B", toLat, toLon, date)).thenReturn(Mono.just(resp));
        when(cache.cache(anyString(), anyString(), any(LocalDate.class), any(EmmaShortTimetableResponse.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(testedObject.getDelayInfo("999", "A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectError(TrainNotInServiceException.class)
                .verify();
    }

    @Test
    void getDelayInfo_missingTimes_usesFallbackScheduleAndReturnsDelayInfo() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        EmmaShortTimetableResponse resp = new EmmaShortTimetableResponse();
        EmmaShortTimetableResponse.Plan plan = new EmmaShortTimetableResponse.Plan();
        EmmaShortTimetableResponse.Itinerary itinerary = new EmmaShortTimetableResponse.Itinerary();
        EmmaShortTimetableResponse.Leg leg = new EmmaShortTimetableResponse.Leg();
        EmmaShortTimetableResponse.Route route = new EmmaShortTimetableResponse.Route();
        EmmaShortTimetableResponse.Trip trip = new EmmaShortTimetableResponse.Trip();

        route.setLongName("SomeRoute");
        trip.setTripShortName("123 night");
        trip.setGtfsId("GTFS-123");

        leg.setRoute(route);
        leg.setTrip(trip);
        leg.setMode("RAIL");

        itinerary.setLegs(List.of(leg));
        plan.setItineraries(List.of(itinerary));
        resp.setPlan(plan);

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", fromLat, fromLon, "B", toLat, toLon, date)).thenReturn(Mono.just(resp));
        when(cache.cache(anyString(), anyString(), any(LocalDate.class), any(EmmaShortTimetableResponse.class)))
                .thenReturn(Mono.empty());

        EmmaShortTrainDetailsResponse details = new EmmaShortTrainDetailsResponse();
        when(gateway.getShortTrainDetails("GTFS-123", date)).thenReturn(Mono.just(details));

        DelayInfo info = new DelayInfo();
        when(delayMapper.mapToDelayInfo(details, "123", date)).thenReturn(Mono.just(List.of(info)));

        StepVerifier.create(testedObject.getDelayInfo("123", "A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectNext(info)
                .verifyComplete();
    }

    @Test
    void getDelayInfo_notFound_mapsToExternalApiException() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        HttpRequest req = mock(HttpRequest.class);
        when(req.getURI()).thenReturn(URI.create("http://x"));

        WebClientResponseException.NotFound ex = mock(WebClientResponseException.NotFound.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(ex.getRequest()).thenReturn(req);

        when(cache.isCached(anyString(), anyString(), any(LocalDate.class))).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable(anyString(), anyDouble(), anyDouble(), anyString(), anyDouble(), anyDouble(), any(LocalDate.class)))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(testedObject.getDelayInfo("1", "A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    void getDelayInfo_requestTimeout_mapsToExternalApiException() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        WebClientRequestException ex = new WebClientRequestException(
                new ConnectTimeoutException("timeout"),
                HttpMethod.GET,
                URI.create("http://timeout.com"),
                new HttpHeaders()
        );

        when(cache.isCached(anyString(), anyString(), any(LocalDate.class))).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable(anyString(), anyDouble(), anyDouble(), anyString(), anyDouble(), anyDouble(), any(LocalDate.class)))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(testedObject.getDelayInfo("1", "A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    void planRoute_validResponse_returnsRoute() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        EmmaTimetableResponse timetable = new EmmaTimetableResponse();
        EmmaTimetableResponse.Plan plan = new EmmaTimetableResponse.Plan();
        EmmaTimetableResponse.Itinerary itinerary = new EmmaTimetableResponse.Itinerary();
        EmmaTimetableResponse.Leg leg = new EmmaTimetableResponse.Leg();

        leg.setMode("RAIL");
        itinerary.setLegs(List.of(leg));
        plan.setItineraries(List.of(itinerary));
        timetable.setPlan(plan);

        List<TrainRouteResponse> mapped = List.of(
                TrainRouteResponse.builder().trains(List.of()).build()
        );

        when(gateway.getTimetable("A", fromLat, fromLon, "B", toLat, toLon, date)).thenReturn(Mono.just(timetable));
        when(routeMapper.mapToRouteResponse(eq(timetable), any(LocalDateTime.class)))
                .thenReturn(Mono.just(mapped));

        StepVerifier.create(testedObject.planRoute("A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void planRoute_webClientError_propagatesOriginalException() {
        LocalDate date = LocalDate.now();
        double fromLat = 47.0, fromLon = 19.0, toLat = 47.5, toLon = 19.5;

        WebClientResponseException ex =
                WebClientResponseException.create(400, "bad", null, null, null, null);

        when(gateway.getTimetable(anyString(), anyDouble(), anyDouble(), anyString(), anyDouble(), anyDouble(), any(LocalDate.class)))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(testedObject.planRoute("A", fromLat, fromLon, "B", toLat, toLon, date))
                .expectError(WebClientResponseException.class)
                .verify();
    }
}
