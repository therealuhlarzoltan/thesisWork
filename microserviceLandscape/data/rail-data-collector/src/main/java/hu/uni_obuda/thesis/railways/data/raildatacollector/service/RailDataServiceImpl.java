package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

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
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class RailDataServiceImpl implements RailDataService {

    private static final Logger LOG = LoggerFactory.getLogger(RailDataServiceImpl.class);

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

        int scheduledDepartureRollover = findRolloverIndex(response.getStations(), localStartTime, station -> station.getScheduledDeparture());
        int scheduledArrivalRollover = findRolloverIndex(response.getStations(), localStartTime, station -> station.getScheduledArrival());
        int realDepartureRollover = findRolloverIndex(response.getStations(), localStartTime, station -> station.getRealDeparture());
        int realArrivalRollover = findRolloverIndex(response.getStations(), localStartTime, station -> station.getRealArrival());

        for (int i = 0; i < response.getStations().size(); i++) {
            ShortTrainDetailsResponse.Station currentStation = response.getStations().get(i);
            DelayInfo delayInfo = DelayInfo.builder()
                    .stationCode(currentStation.getCode())
                    .thirdPartyStationUrl(currentStation.getGetUrl().split("=")[1])
                    .officialStationUrl(currentStation.getUrl())
                    .trainNumber(trainNumber)
                    .date(date)
                    .build();


            if (currentStation.getScheduledArrival() != null && !currentStation.getScheduledArrival().isEmpty()) {
                if (scheduledArrivalRollover != -1 && i >= scheduledArrivalRollover) {
                    delayInfo.setScheduledArrival(parseTimeSafe(currentStation.getScheduledArrival()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setScheduledArrival(parseTimeSafe(currentStation.getScheduledArrival()).atDate(date).toString());
                }
            }

            if (currentStation.getScheduledDeparture() != null && !currentStation.getScheduledDeparture().isEmpty()) {
                if (scheduledDepartureRollover != -1 && i >= scheduledDepartureRollover) {
                    delayInfo.setScheduledDeparture(parseTimeSafe(currentStation.getScheduledDeparture()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setScheduledDeparture(parseTimeSafe(currentStation.getScheduledDeparture()).atDate(date).toString());
                }
            }

            if (currentStation.getRealArrival() != null && !currentStation.getRealArrival().isEmpty()) {
                if (realArrivalRollover != -1 && i >= realArrivalRollover) {
                    delayInfo.setActualArrival((parseTimeSafe(currentStation.getRealArrival()).atDate(date).plusDays(1).toString()));
                } else {
                    delayInfo.setActualArrival(parseTimeSafe(currentStation.getRealArrival()).atDate(date).toString());
                }
            }

            if (currentStation.getRealDeparture() != null && !currentStation.getRealDeparture().isEmpty()) {
                if (realDepartureRollover != -1 && i >= realDepartureRollover) {
                    delayInfo.setActualDeparture(parseTimeSafe(currentStation.getRealDeparture()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setActualDeparture(parseTimeSafe(currentStation.getRealDeparture()).atDate(date).toString());
                }
            }

            delayInfo.setArrivalDelay(calculateDelay(delayInfo.getScheduledArrival(), delayInfo.getActualArrival()));
            delayInfo.setDepartureDelay(calculateDelay(delayInfo.getScheduledDeparture(), delayInfo.getActualDeparture()));

            delayInfos.add(delayInfo);
        }
        return Mono.just(delayInfos);
    }

    private int findRolloverIndex(List<ShortTrainDetailsResponse.Station> stations, LocalTime startTime, Function<ShortTrainDetailsResponse.Station, String> propertyGetter) {
        LocalTime previousTime = startTime;
        for (int i = 0; i < stations.size(); i++) {
            ShortTrainDetailsResponse.Station currentStation = stations.get(i);
            String timeProperty = propertyGetter.apply(currentStation);
            if (timeProperty != null && !timeProperty.isBlank()) {
                LocalTime currentTime = parseTimeSafe(timeProperty);
                if (currentTime.isBefore(previousTime)) {
                    return i;
                }
                previousTime = parseTimeSafe(timeProperty);
            }
        }
        return -1;
    }

    private static LocalTime parseTimeSafe(String timeStr) {
        return LocalTime.parse(timeStr.equals("24:00") ? "00:00" : timeStr);
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
