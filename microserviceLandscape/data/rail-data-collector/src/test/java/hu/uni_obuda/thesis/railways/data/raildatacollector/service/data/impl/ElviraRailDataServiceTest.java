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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.web.reactive.function.client.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ElviraRailDataServiceTest {

    private ElviraRailDataGateway gateway;
    private ElviraTimetableCache cache;
    private ElviraDelayMapper delayMapper;
    private ElviraRouteMapper routeMapper;

    private ElviraRailDataServiceImpl service;

    @BeforeEach
    public void setup() {
        gateway = mock(ElviraRailDataGateway.class);
        cache = mock(ElviraTimetableCache.class);
        delayMapper = mock(ElviraDelayMapper.class);
        routeMapper = mock(ElviraRouteMapper.class);

        service = new ElviraRailDataServiceImpl(gateway, cache, delayMapper, routeMapper);
    }

    @Test
    public void getDelayInfo_cacheHit_returnsDelayInfo() {
        LocalDate date = LocalDate.now();

        ElviraShortTimetableResponse resp = new ElviraShortTimetableResponse(
                List.of(new ElviraShortTimetableResponse.TimetableEntry(
                        "10:00",
                        "11:00",
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

        StepVerifier.create(service.getDelayInfo("123", "A", "B", date))
                .expectNext(info)
                .verifyComplete();
    }

    @Test
    public void getDelayInfo_cacheMiss_getsAndCachesTimetable() {
        LocalDate date = LocalDate.now();

        ElviraShortTimetableResponse resp = new ElviraShortTimetableResponse(
                List.of(new ElviraShortTimetableResponse.TimetableEntry(
                        "09:00",
                        "09:30",
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

        StepVerifier.create(service.getDelayInfo("456", "A", "B", date))
                .expectNext(info)
                .verifyComplete();
    }

    @Test
    public void getDelayInfo_emptyTimetable_throwsFormatException() {
        LocalDate date = LocalDate.now();

        when(cache.isCached("A", "B", date)).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable("A", "B", date))
                .thenReturn(Mono.just(new ElviraShortTimetableResponse(List.of())));

        StepVerifier.create(service.getDelayInfo("999", "A", "B", date))
                .expectError(ExternalApiFormatMismatchException.class)
                .verify();
    }

    @Test
    public void getDelayInfo_trainNotFound_throwsTrainNotInService() {
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

        StepVerifier.create(service.getDelayInfo("999", "A", "B", date))
                .expectError(TrainNotInServiceException.class)
                .verify();
    }

    @Test
    public void getDelayInfo_malformedTimes_usesFallbackSchedule() {
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

        ElviraShortTrainDetailsResponse details = new ElviraShortTrainDetailsResponse();
        when(gateway.getShortTrainDetails("http://url")).thenReturn(Mono.just(details));

        DelayInfo info = new DelayInfo();
        when(delayMapper.mapToDelayInfo(details, "123", date)).thenReturn(Mono.just(List.of(info)));

        StepVerifier.create(service.getDelayInfo("123", "A", "B", date))
                .expectNext(info)
                .verifyComplete();
    }

    @Test
    public void getDelayInfo_notFound_mapsToExternalApiException() throws Exception {
        LocalDate date = LocalDate.now();

        HttpRequest req = mock(HttpRequest.class);
        when(req.getURI()).thenReturn(new URI("http://x"));

        WebClientResponseException ex =
                WebClientResponseException.create(404, "NF", null, null, null);
        when(ex.getRequest()).thenReturn(req);

        when(cache.isCached(any(), any(), any())).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(service.getDelayInfo("1", "A", "B", date))
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    public void getDelayInfo_requestTimeout_mapsToExternalApiException() throws Exception {
        LocalDate date = LocalDate.now();

        WebClientRequestException ex = new WebClientRequestException(
                new ConnectTimeoutException("timeout"),
                HttpMethod.GET,
                URI.create("http://timeoout.com"),
                new HttpHeaders()
        );

        when(cache.isCached(any(), any(), any())).thenReturn(Mono.just(false));
        when(gateway.getShortTimetable(any(), any(), any())).thenReturn(Mono.error(ex));

        StepVerifier.create(service.getDelayInfo("1", "A", "B", date))
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    public void planRoute_validResponse_returnsRoute() {
        LocalDate date = LocalDate.now();
        ElviraTimetableResponse tt = new ElviraTimetableResponse();

        List<TrainRouteResponse> mapped = List.of(
                TrainRouteResponse.builder().trains(List.of()).build()
        );

        when(gateway.getTimetable("A", "B", date)).thenReturn(Mono.just(tt));
        when(routeMapper.mapToRouteResponse(tt, date)).thenReturn(Mono.just(mapped));

        StepVerifier.create(service.planRoute("A", "B", date))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void planRoute_webClientError_mapsToExternalApiException() throws Exception {
        LocalDate date = LocalDate.now();

        HttpRequest req = mock(HttpRequest.class);
        when(req.getURI()).thenReturn(new URI("http://xy"));

        WebClientResponseException ex =
                WebClientResponseException.create(400, "bad", null, null, null);
        when(ex.getRequest()).thenReturn(req);

        when(gateway.getTimetable(anyString(), anyString(), any()))
                .thenReturn(Mono.error(ex));

        StepVerifier.create(service.planRoute("A", "B", date))
                .expectError(ExternalApiException.class)
                .verify();
    }
}
