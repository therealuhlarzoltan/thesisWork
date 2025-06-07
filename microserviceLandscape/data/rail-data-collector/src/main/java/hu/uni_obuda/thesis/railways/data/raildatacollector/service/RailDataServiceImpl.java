package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway.RailDelayGateway;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.components.TimetableCache;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URI;
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

@Service
@RequiredArgsConstructor
public class RailDataServiceImpl implements RailDataService {

    private static final Logger LOG = LoggerFactory.getLogger(RailDataServiceImpl.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final int EARLY_ARRIVAL_THRESHOLD_HOURS = 12;

    private final RailDelayGateway gateway;
    private final TimetableCache timetableCache;

    @Override
    public Flux<DelayInfo> getDelayInfo(String trainNumber, String from, String to, LocalDate date) {
        return  timetableCache.isCached(from, to, date)
                .flatMap(isCached -> {
                    if (Boolean.TRUE.equals(isCached)) {
                        LOG.info("Timetable with start station {} and end station {} on date {} is already cached, reusing cached object", from, to, date);
                        return timetableCache.get(from, to, date);
                    } else {
                        LOG.info("Getting timetable with start station {} and end station {} on date {}", from, to, date);
                        return gateway.getShortTimetable(from, to, date)
                                .flatMap(response -> {
                                    if (!response.getTimetable().isEmpty()) {
                                        LOG.info("Caching timetable with start station {} and end station {} on date {}", from, to, date);
                                        return timetableCache.cache(from, to, date, response)
                                                .thenReturn(response);
                                    } else {
                                        LOG.warn("Got an empty timetable with start station {} and end station {} on date {}", from, to, date);
                                        return Mono.just(response);
                                    }
                               });
                    }
                })
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(timetableResponse -> extractTrainUri(timetableResponse, trainNumber, date))
                .flatMap(trainUri -> gateway.getShortTrainDetails(trainUri))
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(shortTrainDetailsResponse ->  mapToDelayInfo(shortTrainDetailsResponse, trainNumber, date))
                .flatMapMany(Flux::fromIterable);
    }

    private static Mono<String> extractTrainUri(ShortTimetableResponse response, String trainNumber, LocalDate date) {
        LOG.info("Extracting train URI for train number {} on date {}", trainNumber, date);
        return !response.getTimetable().isEmpty() ? response.getTimetable().stream()
                .flatMap(entry -> entry.getDetails().stream())
                .filter(details -> details.getTrainInfo().getCode().equals(trainNumber))
                .findFirst()
                .map(details -> Mono.just(details.getTrainInfo().getUrl()))
                .orElse(Mono.error(new TrainNotInServiceException(trainNumber, date)))
                : Mono.error(new ExternalApiFormatMismatchException("Received an empty response", null));
    }

    private Mono<List<DelayInfo>> mapToDelayInfo(ShortTrainDetailsResponse response, String trainNumber, LocalDate date) {
        if (response.getStations().getLast().getRealArrival() == null || response.getStations().getLast().getRealArrival().isBlank()) {
            LOG.warn("Returning empty station list because train {} hasn't arrived yet", trainNumber);
            return Mono.just(Collections.emptyList());
        }
        String startTime = response.getStations().getFirst().getScheduledDeparture();
        LocalTime localStartTime;
        try {
            localStartTime = LocalTime.parse(startTime);
        } catch (DateTimeParseException e) {
            LOG.error("Could not parse scheduled departure at first stop for train {}", trainNumber);
            return Mono.error(e);
        }
        List<DelayInfo> delayInfos = new ArrayList<>();

        LocalDate currentRealArrivalDate = date;
        LocalDate currentRealDepartureDate = date;
        LocalDate currentScheduledArrivalDate = date;
        LocalDate currentScheduledDepartureDate = date;

        LocalTime previousRealArrivalTime = null;
        LocalTime previousRealDepartureTime = null;
        LocalTime previousScheduledArrivalTime = null;
        LocalTime previousScheduledDepartureTime = null;

        for (var station : response.getStations()) {
            DelayInfo delayInfo = DelayInfo.builder()
                    .stationCode(station.getCode())
                    .thirdPartyStationUrl(station.getGetUrl().split("=")[1])
                    .officialStationUrl(station.getUrl())
                    .trainNumber(trainNumber)
                    .date(date)
                    .build();

            // --- Scheduled Arrival ---
            String schedArr = station.getScheduledArrival();
            if (schedArr != null && !schedArr.isBlank()) {
                LocalTime schedArrTime = parseTimeSafe(schedArr);
                if (previousScheduledArrivalTime != null && schedArrTime.isBefore(previousScheduledArrivalTime)) {
                    currentScheduledArrivalDate = currentScheduledArrivalDate.plusDays(1);
                }
                previousScheduledArrivalTime = schedArrTime;
                delayInfo.setScheduledArrival(schedArrTime.atDate(currentScheduledArrivalDate).toString());
            }

            // --- Scheduled Departure ---
            String schedDep = station.getScheduledDeparture();
            if (schedDep != null && !schedDep.isBlank()) {
                LocalTime schedDepTime = parseTimeSafe(schedDep);
                if (previousScheduledDepartureTime != null && schedDepTime.isBefore(previousScheduledDepartureTime)) {
                    currentScheduledDepartureDate = currentScheduledDepartureDate.plusDays(1);
                }
                previousScheduledDepartureTime = schedDepTime;
                delayInfo.setScheduledDeparture(schedDepTime.atDate(currentScheduledDepartureDate).toString());
            }

            // --- Real Arrival ---
            String realArr = station.getRealArrival();
            if (realArr != null && !realArr.isBlank()) {
                LocalTime realArrTime = parseTimeSafe(realArr);
                if (previousRealArrivalTime != null) {
                    if (realArrTime.isBefore(previousRealArrivalTime)) {
                        currentRealArrivalDate = currentRealArrivalDate.plusDays(1);
                    }
                } else if (previousScheduledArrivalTime != null) {
                    // Handle delayed trains arriving next day
                    if (realArrTime.isBefore(previousScheduledArrivalTime)) {
                        currentRealArrivalDate = currentRealArrivalDate.plusDays(1);
                    } else {
                        Duration diff = Duration.between(previousScheduledArrivalTime, realArrTime);
                        if (diff.toMinutes() >= EARLY_ARRIVAL_THRESHOLD_HOURS * 60) {  // tweak threshold if needed
                            currentRealArrivalDate = currentRealArrivalDate.plusDays(1);
                        }
                    }
                }
                previousRealArrivalTime = realArrTime;
                delayInfo.setActualArrival(realArrTime.atDate(currentRealArrivalDate).toString());
            }

            // --- Real Departure ---
            String realDep = station.getRealDeparture();
            if (realDep != null && !realDep.isBlank()) {
                LocalTime realDepTime = parseTimeSafe(realDep);
                if (previousRealDepartureTime != null) {
                    if (realDepTime.isBefore(previousRealDepartureTime)) {
                        currentRealDepartureDate = currentRealDepartureDate.plusDays(1);
                    }
                } else if (previousScheduledDepartureTime != null) {
                    // Handle delayed departures next day
                    if (realDepTime.isBefore(previousScheduledDepartureTime)) {
                        currentRealDepartureDate = currentRealDepartureDate.plusDays(1);
                    } else {
                        Duration diff = Duration.between(previousScheduledDepartureTime, realDepTime);
                        if (diff.toHours() >= EARLY_ARRIVAL_THRESHOLD_HOURS * 60) {
                            currentRealDepartureDate = currentRealDepartureDate.plusDays(1);
                        }
                    }
                }
                previousRealDepartureTime = realDepTime;
                delayInfo.setActualDeparture(realDepTime.atDate(currentRealDepartureDate).toString());
            }

            // --- Delay calculation ---
            delayInfo.setArrivalDelay(calculateDelay(delayInfo.getScheduledArrival(), delayInfo.getActualArrival()));
            delayInfo.setDepartureDelay(calculateDelay(delayInfo.getScheduledDeparture(), delayInfo.getActualDeparture()));

            delayInfos.add(delayInfo);
        }
        return Mono.just(delayInfos);
    }

    private static LocalTime parseTimeSafe(String timeStr) {
        return LocalTime.parse(timeStr.equals("24:00") ? "00:00" : timeStr);
    }

    private LocalDateTime toLocalDateTime(String scheduled, String actual, LocalDate date) {
        if (scheduled == null || scheduled.isBlank() || actual == null || actual.isBlank() || date == null) {
            return null;
        }
        try {
            LocalDateTime actualDate = LocalDateTime.of(date, LocalTime.parse(actual));
            LocalDateTime scheduledDate = LocalDateTime.of(date, LocalTime.parse(scheduled));
            if (!actualDate.isBefore(scheduledDate)) {
                //On-time or late (same day) arrival/departure
                return actualDate;
            } else {
                // Early or late (different day) arrival/departure
                Duration duration = Duration.between(actualDate, scheduledDate);
                return duration.toMinutes() < EARLY_ARRIVAL_THRESHOLD_HOURS * 60 ? actualDate : actualDate.plusDays(1);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Integer calculateDelay(String scheduled, String actual) {
        if (scheduled == null || scheduled.isBlank() || actual == null || actual.isBlank()) {
            return null;
        } else {
            try {
                LocalDateTime scheduledDate = LocalDateTime.parse(scheduled, DateTimeFormatter.ISO_DATE_TIME);
                LocalDateTime actualDate = LocalDateTime.parse(actual, DateTimeFormatter.ISO_DATE_TIME);
                return (int) Duration.between(scheduledDate, actualDate).toMinutes();
            } catch (DateTimeParseException e) {
                return null;
            }
        }
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
}
