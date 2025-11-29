package hu.uni_obuda.thesis.railways.data.raildatacollector.service.data.impl;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway.ElviraRailDataGateway;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.ElviraTimetableCache;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data.ElviraDelayMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data.ElviraRouteMapper;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;

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
import org.springframework.web.reactive.function.client.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElviraRailDataServiceTest {

    @Mock
    private ElviraRailDataGateway gateway;
    @Mock
    private ElviraTimetableCache cache;
    @Mock
    private ElviraDelayMapper delayMapper;
    @Mock
    private ElviraRouteMapper routeMapper;

    @InjectMocks
    private ElviraRailDataServiceImpl testedObject;

    @Test
    void getDelayInfo_cacheHit_returnsDelayInfo() {
        LocalDate date = LocalDate.now();

        LocalTime now = LocalTime.now();
        LocalTime departureTime = now.minusHours(2);
        LocalTime arrivalTime = now.minusHours(1);

        ElviraShortTimetableResponse resp = new ElviraShortTimetableResponse(
                List.of(new ElviraShortTimetableResponse.TimetableEntry(
                        departureTime.toString(),
                        arrivalTime.toString(),
                        List.of(new ElviraShortTimetableResponse.TrainDetail(
                                new ElviraShortTimetableResponse.TrainInfo("http://train", "", "123", "")
                        ))
                ))
        );

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(true));
        when(cache.get("A", "B", date)).thenReturn(Mono.just(resp));

        ElviraShortTrainDetailsResponse details = new ElviraShortTrainDetailsResponse();
        when(gateway.getShortTrainDetails("http://train")).thenReturn(Mono.just(details));

        DelayInfo info = DelayInfo.builder().trainNumber("123").build();
        when(delayMapper.mapToDelayInfo(details, "123", date)).thenReturn(Mono.just(List.of(info)));

        StepVerifier.create(testedObject.getDelayInfo("123", "A", "B", date))
                .expectNext(info)
                .verifyComplete();
    }

    @Test
    void getDelayInfo_cacheMiss_getsAndCachesTimetable() {
        LocalDate date = LocalDate.now();

        LocalTime now = LocalTime.now();
        LocalTime departureTime = now.minusHours(3);
        LocalTime arrivalTime = now.minusHours(2);

        ElviraShortTimetableResponse resp = new ElviraShortTimetableResponse(
                List.of(new ElviraShortTimetableResponse.TimetableEntry(
                        departureTime.toString(),
                        arrivalTime.toString(),
                        List.of(new ElviraShortTimetableResponse.TrainDetail(
                                new ElviraShortTimetableResponse.TrainInfo("http://train", "", "456", "")
                        ))
                ))
        );

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", "B", date)).thenReturn(Mono.just(resp));
        when(cache.cache(eq("A"), eq("B"), eq(date), eq(resp))).thenReturn(Mono.empty());

        ElviraShortTrainDetailsResponse details = new ElviraShortTrainDetailsResponse();
        when(gateway.getShortTrainDetails("http://train")).thenReturn(Mono.just(details));

        DelayInfo info = DelayInfo.builder().trainNumber("456").build();
        when(delayMapper.mapToDelayInfo(details, "456", date)).thenReturn(Mono.just(List.of(info)));

        StepVerifier.create(testedObject.getDelayInfo("456", "A", "B", date))
                .expectNext(info)
                .verifyComplete();
    }

    @Test
    void getDelayInfo_emptyTimetable_throwsFormatException() {
        LocalDate date = LocalDate.now();

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", "B", date))
                .thenReturn(Mono.just(new ElviraShortTimetableResponse(List.of())));

        StepVerifier.create(testedObject.getDelayInfo("999", "A", "B", date))
                .expectError(ExternalApiFormatMismatchException.class)
                .verify();
    }

    @Test
    void getDelayInfo_trainNotFound_throwsTrainNotInService() {
        LocalDate date = LocalDate.now();

        ElviraShortTimetableResponse resp = new ElviraShortTimetableResponse(
                List.of(new ElviraShortTimetableResponse.TimetableEntry(
                        "10:00",
                        "11:00",
                        List.of(new ElviraShortTimetableResponse.TrainDetail(
                                new ElviraShortTimetableResponse.TrainInfo("http://train", "", "AAA", "")
                        ))
                ))
        );

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", "B", date)).thenReturn(Mono.just(resp));

        when(cache.cache(anyString(),
                anyString(),
                any(LocalDate.class),
                any(ElviraShortTimetableResponse.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(testedObject.getDelayInfo("999", "A", "B", date))
                .expectError(TrainNotInServiceException.class)
                .verify();
    }

    @Test
    void getDelayInfo_malformedTimes_usesFallbackSchedule() {
        LocalDate date = LocalDate.now();

        ElviraShortTimetableResponse resp = new ElviraShortTimetableResponse(
                List.of(new ElviraShortTimetableResponse.TimetableEntry(
                        "xx:yy",
                        "zz:ww",
                        List.of(new ElviraShortTimetableResponse.TrainDetail(
                                new ElviraShortTimetableResponse.TrainInfo("http://url", "", "123", "")
                        ))
                ))
        );

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", "B", date)).thenReturn(Mono.just(resp));

        when(cache.cache(anyString(),
                anyString(),
                any(LocalDate.class),
                any(ElviraShortTimetableResponse.class)))
                .thenReturn(Mono.empty());

        ElviraShortTrainDetailsResponse details = new ElviraShortTrainDetailsResponse();
        when(gateway.getShortTrainDetails("http://url")).thenReturn(Mono.just(details));

        DelayInfo info = new DelayInfo();
        when(delayMapper.mapToDelayInfo(details, "123", date))
                .thenReturn(Mono.just(List.of(info)));

        StepVerifier.create(testedObject.getDelayInfo("123", "A", "B", date))
                .expectNext(info)
                .verifyComplete();
    }

    @Test
    void getDelayInfo_notFound_mapsToExternalApiException() {
        LocalDate date = LocalDate.now();

        HttpRequest req = mock(HttpRequest.class);
        when(req.getURI()).thenReturn(URI.create("http://x"));

        WebClientResponseException.NotFound ex = mock(WebClientResponseException.NotFound.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(ex.getRequest()).thenReturn(req);

        when(cache.isCached(any(), any(), any())).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(testedObject.getDelayInfo("1", "A", "B", date))
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    void getDelayInfo_requestTimeout_mapsToExternalApiException() {
        LocalDate date = LocalDate.now();

        WebClientRequestException ex = new WebClientRequestException(
                new ConnectTimeoutException("timeout"),
                HttpMethod.GET,
                URI.create("http://timeoout.com"),
                new HttpHeaders()
        );

        when(cache.isCached(any(), any(), any())).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(testedObject.getDelayInfo("1", "A", "B", date))
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    void planRoute_validResponse_returnsRoute() {
        LocalDate date = LocalDate.now();
        ElviraTimetableResponse tt = new ElviraTimetableResponse();

        List<TrainRouteResponse> mapped = List.of(
                TrainRouteResponse.builder().trains(List.of()).build()
        );

        when(gateway.getTimetable("A", "B", date)).thenReturn(Mono.just(tt));
        when(routeMapper.mapToRouteResponse(tt, date)).thenReturn(Mono.just(mapped));

        StepVerifier.create(testedObject.planRoute("A", "B", date))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void planRoute_webClientError_mapsToExternalApiException() {
        LocalDate date = LocalDate.now();

        HttpRequest req = mock(HttpRequest.class);
        when(req.getURI()).thenReturn(URI.create("http://xy"));

        WebClientResponseException.BadRequest ex = mock(WebClientResponseException.BadRequest.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(ex.getRequest()).thenReturn(req);

        when(gateway.getTimetable(anyString(), anyString(), any()))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(testedObject.planRoute("A", "B", date))
                .expectError(ExternalApiException.class)
                .verify();
    }
}
