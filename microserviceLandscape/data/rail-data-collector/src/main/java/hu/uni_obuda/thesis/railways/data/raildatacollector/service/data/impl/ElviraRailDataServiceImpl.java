package hu.uni_obuda.thesis.railways.data.raildatacollector.service.data.impl;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway.ElviraRailDataGateway;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.ElviraTimetableCache;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data.ElviraDelayMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data.ElviraRouteMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.data.ElviraRailDataService;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Profile("data-source-elvira")
@Service
@Slf4j
@RequiredArgsConstructor
public class ElviraRailDataServiceImpl implements ElviraRailDataService {

    private final ElviraRailDataGateway gateway;
    private final ElviraTimetableCache timetableCache;
    private final ElviraDelayMapper delayMapper;
    private final ElviraRouteMapper routeMapper;

    @Override
    public Flux<DelayInfo> getDelayInfo(String trainNumber, String from, String to, LocalDate date) {
        return  timetableCache.isCached(from, to, date)
                .flatMap(isCached -> {
                    if (Boolean.TRUE.equals(isCached)) {
                        log.info("Timetable with start station {} and end station {} on date {} is already cached, reusing cached object", from, to, date);
                        return timetableCache.get(from, to, date);
                    } else {
                        log.info("Getting timetable with start station {} and end station {} on date {}", from, to, date);
                        return gateway.getShortTimetable(from, to, date)
                                .flatMap(response -> {
                                    if (!response.getTimetable().isEmpty()) {
                                        log.info("Caching timetable with start station {} and end station {} on date {}", from, to, date);
                                        return timetableCache.cache(from, to, date, response)
                                                .thenReturn(response);
                                    } else {
                                        log.warn("Got an empty timetable with start station {} and end station {} on date {}", from, to, date);
                                        return Mono.just(response);
                                    }
                               });
                    }
                })
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(timetableResponse -> extractTimetableEntry(timetableResponse, trainNumber, date))
                .flatMap(entry -> extractSchedule(entry, trainNumber))
                .flatMap(this::checkSchedule)
                .flatMap(entry -> extractTrainUri(entry, trainNumber, date))
                .flatMap(gateway::getShortTrainDetails)
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(shortTrainDetailsResponse ->  delayMapper.mapToDelayInfo(shortTrainDetailsResponse, trainNumber, date))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<TrainRouteResponse> planRoute(String from, String to, LocalDate date) {
        return gateway.getTimetable(from, to, date)
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(timetableResponse ->  routeMapper.mapToRouteResponse(timetableResponse, date))
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<ElviraShortTimetableResponse.TimetableEntry> checkSchedule(Tuple4<LocalTime, LocalTime, String, ElviraShortTimetableResponse.TimetableEntry> schedule) {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.MIDNIGHT) && now.isBefore(LocalTime.MIDNIGHT.plusHours(4))) {
            log.info("Not checking schedule in the early hours of the day, proceeding...");
            return Mono.just(schedule.getT4());
        }
        if (now.isBefore(schedule.getT1())) {
            log.warn("Train {} has not departed yet according to schedule, aborting...", schedule.getT3());
            return Mono.empty();
        } else if (now.isBefore(schedule.getT2())) {
            log.warn("Train {} has not arrived yet according to schedule, aborting...", schedule.getT3());
            return Mono.empty();
        } else {
            log.info("Train should have arrived by now according to schedule, proceeding...");
            return Mono.just(schedule.getT4());
        }
    }

    private Mono<Tuple4<LocalTime, LocalTime, String, ElviraShortTimetableResponse.TimetableEntry>> extractSchedule(ElviraShortTimetableResponse.TimetableEntry entry, String trainNumber) {
        String departureString = entry.getStartTime();
        String arrivalString = entry.getDestinationTime();
        LocalTime arrivalTime;
        LocalTime departureTime;
        try {
            departureTime = parseTimeSafely(departureString);
            arrivalTime = parseTimeSafely(arrivalString);
        } catch (Exception e) {
            log.error("Could not extract scheduled departure and or arrival for train {}", trainNumber, e);
            log.warn("Assuming train {} has just arrived", trainNumber);
            return Mono.just(Tuples.of(LocalTime.MIDNIGHT, LocalTime.now(), trainNumber, entry));
        }
        return Mono.just(Tuples.of(departureTime, arrivalTime, trainNumber, entry));
    }

    private static Mono<ElviraShortTimetableResponse.TimetableEntry> extractTimetableEntry(ElviraShortTimetableResponse response, String trainNumber, LocalDate date) {
        log.info("Extracting timetable entry for train number {} on date {}", trainNumber, date);
        return !response.getTimetable().isEmpty() ? response.getTimetable().stream()
                .filter(entry -> entry.getDetails().stream()
                        .anyMatch(details ->
                                details.getTrainInfo().getCode().equals(trainNumber)))
                .findFirst()
                .map(Mono::just)
                .orElse(Mono.error(new TrainNotInServiceException(trainNumber, date)))
                : Mono.error(new ExternalApiFormatMismatchException("Received an empty response", null));
    }

    private static Mono<String> extractTrainUri(ElviraShortTimetableResponse.TimetableEntry entry, String trainNumber, LocalDate date) {
        log.info("Extracting train URI for train number {}", trainNumber);
        var details = entry.getDetails();
        if (details == null) {
            return Mono.error(new TrainNotInServiceException(trainNumber, date));
        }
        return Flux.fromIterable(details)
                .filter(detail -> trainNumber.equals(detail.getTrainInfo().getCode()))
                .next()
                .map(detail -> detail.getTrainInfo().getUrl())
                .switchIfEmpty(Mono.error(new TrainNotInServiceException(trainNumber, date)));
    }


    private ExternalApiException mapNotFoundToExternalApiException(WebClientResponseException.NotFound notFound) {
        return new ExternalApiException(notFound.getStatusCode(), extractUrlFromRequest(notFound.getRequest()));
    }

    private ExternalApiException mapBadRequestToExternalApiException(WebClientResponseException.BadRequest badRequest) {
        return new ExternalApiException(badRequest.getStatusCode(), extractUrlFromRequest(badRequest.getRequest()));
    }

    private URL extractUrlFromRequest(HttpRequest request) {
        try {
            return request.getURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private ApiException mapWebClientRequestExceptionToApiException(WebClientRequestException requestException) {
        URL url;
        try {
            url = requestException.getUri().toURL();
        } catch (MalformedURLException _) {
            url = null;
        }
        Throwable cause = requestException.getCause();
        if (cause instanceof ConnectTimeoutException || cause instanceof ReadTimeoutException || cause instanceof WriteTimeoutException) {
            return new ExternalApiException(HttpStatus.SERVICE_UNAVAILABLE, url, "Connection timed out");
        }
        return new InternalApiException(requestException.getMessage(), url);
    }

    private static LocalTime parseTimeSafely(String timeStr) {
        return LocalTime.parse(timeStr.equals("24:00") ? "00:00" : timeStr);
    }
}
