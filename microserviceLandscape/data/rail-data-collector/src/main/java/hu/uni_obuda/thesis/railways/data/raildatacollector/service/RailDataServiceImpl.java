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
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RailDataServiceImpl implements RailDataService {

    private static final Logger LOG = LoggerFactory.getLogger(RailDataServiceImpl.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

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
                                        return timetableCache.cache(from, to, date, response)
                                                .thenReturn(response);
                                    } else {
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
        return !response.getTimetable().isEmpty() ? response.getTimetable().stream()
                .flatMap(entry -> entry.getDetails().stream())
                .filter(details -> details.getTrainInfo().getCode().equals(trainNumber))
                .findFirst()
                .map(details -> Mono.just(details.getTrainInfo().getUrl()))
                .orElse(Mono.error(new TrainNotInServiceException(trainNumber, date)))
                : Mono.error(new ExternalApiFormatMismatchException("Received an empty response", null));
    }

    private static Mono<List<DelayInfo>> mapToDelayInfo(ShortTrainDetailsResponse response, String trainNumber, LocalDate date) {
        if (response.getStations().getLast().getRealArrival() == null || response.getStations().getLast().getRealArrival().isBlank()) {
            return Mono.just(Collections.emptyList());
        }
        return Mono.just(response.getStations().stream().map(station -> mapStationInfoToDelayInfo(station, trainNumber, date)).toList());
    }

    private static DelayInfo mapStationInfoToDelayInfo(ShortTrainDetailsResponse.Station station, String trainNumber, LocalDate date) {
        Integer arrivalDelay;
        Integer departureDelay;
        if (station.getRealArrival() == null && station.getRealDeparture() == null) {
            arrivalDelay = null;
            departureDelay = null;
        } else {
            if (station.getRealArrival() != null && station.getScheduledArrival() != null) {
                arrivalDelay = calculateArrivalDelay(station.getScheduledArrival(), station.getRealArrival());
            } else {
                arrivalDelay = null;
            }
            if (station.getRealDeparture() != null && station.getScheduledDeparture() != null) {
                departureDelay = calculateDepartureDelay(station.getScheduledDeparture(), station.getRealDeparture());
            } else {
                departureDelay = null;
            }
        }

        return DelayInfo.builder().stationCode(station.getCode()).thirdPartyStationUrl(station.getGetUrl().split("=")[1])
                .officialStationUrl(station.getUrl())
                .trainNumber(trainNumber)
                .scheduledDeparture(station.getScheduledDeparture())
                .scheduledArrival(station.getScheduledArrival())
                .actualArrival(station.getRealArrival())
                .actualDeparture(station.getRealDeparture())
                .arrivalDelay(arrivalDelay)
                .departureDelay(departureDelay)
                .date(date)
                .build();
    }

    private static Integer calculateArrivalDelay(String scheduledArrival, String realArrival) {
       if (!scheduledArrival.isBlank() && !realArrival.isBlank()) {
           return convertToMinutes(realArrival) - convertToMinutes(scheduledArrival);
       } else {
            return null;
       }
    }

    private static Integer calculateDepartureDelay(String scheduledDeparture, String realDeparture) {
        if (!scheduledDeparture.isBlank() && !realDeparture.isBlank()) {
            return convertToMinutes(realDeparture) - convertToMinutes(scheduledDeparture);
        } else {
            return null;
        }
    }

    private static int convertToMinutes(String time) {
        LocalTime localTime = LocalTime.parse(time, dateTimeFormatter);
        return localTime.getHour() * 60 + localTime.getMinute();
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
