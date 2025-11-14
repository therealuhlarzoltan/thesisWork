package hu.uni_obuda.thesis.railways.data.raildatacollector.service.data.impl;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway.EmmaRailDelayGateway;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.*;
import hu.uni_obuda.thesis.railways.data.raildatacollector.component.cache.EmmaTimetableCache;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data.EmmaDelayMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data.EmmaRouteMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.data.EmmaRailDataService;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Profile("data-source-emma")
@Service
@Slf4j
@RequiredArgsConstructor
public class EmmaRailDataServiceImpl implements EmmaRailDataService {

    private static final LocalTime THREE_AM = LocalTime.MIDNIGHT.plusHours(3);

    private final EmmaRailDelayGateway gateway;
    private final EmmaTimetableCache timetableCache;
    private final EmmaDelayMapper delayMapper;
    private final EmmaRouteMapper routeMapper;

    @Override
    public Flux<DelayInfo> getDelayInfo(String trainNumber, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return  timetableCache.isCached(from, to, date)
                .flatMap(isCached -> {
                    if (Boolean.TRUE.equals(isCached)) {
                        log.info("Timetable with start station {} and end station {} on date {} is already cached, reusing cached object", from, to, date);
                        return timetableCache.get(from, to, date);
                    } else {
                        log.info("Getting timetable with start station {} and end station {} on date {}", from, to, date);
                        return gateway.getShortTimetable(from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date)
                                .flatMap(response -> {
                                    if (!response.getPlan().getItineraries().isEmpty()) {
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
                .flatMap(this::checkOvernightTrain)
                .flatMap(entry -> extractTrainId(entry, trainNumber, date))
                .flatMap(trainId -> gateway.getShortTrainDetails(trainId, date))
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(shortTrainDetailsResponse ->  delayMapper.mapToDelayInfo(shortTrainDetailsResponse, trainNumber, date))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<TrainRouteResponse> planRoute(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        log.info("Getting routes with start station {} and end station {} on date {}", from, to, date);
        return gateway.getTimetable(from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date)
                .flatMap(routesResponse -> routeMapper.mapToRouteResponse(routesResponse, LocalDateTime.now()))
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<EmmaShortTimetableResponse.Leg> checkSchedule(Tuple4<LocalTime, LocalTime, String, EmmaShortTimetableResponse.Leg> schedule) {
        LocalTime now = LocalTime.now();
        if (now.equals(LocalTime.MIDNIGHT) || now.isBefore(THREE_AM)) {
            log.info("Checking schedule in the early hours of day....");
            if (!schedule.getT2().equals(LocalTime.MIDNIGHT) && !schedule.getT2().isBefore(THREE_AM)) {
                log.error("Delay data is not available for train {} because it arrived yesterday, aborting...", schedule.getT3());
                return Mono.error(new TrainNotInServiceException(schedule.getT3(), LocalDate.now().minusDays(1)));
            }
            log.info("Not checking schedule int the early hours of day, proceeding...");
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

    private Mono<EmmaShortTimetableResponse.Leg> checkOvernightTrain(EmmaShortTimetableResponse.Leg transportLeg) {
        LocalTime startTime = transportLeg.getStartLocalTime();
        LocalTime endTime = transportLeg.getEndLocalTime();
        if (startTime == null || endTime == null) {
            log.warn("Could not infer schedule, proceeding...");
            return Mono.just(transportLeg);
        }
        if (endTime.isBefore(THREE_AM)) {
            if (endTime.isBefore(startTime)) { // overnight train, proceed immediately
                log.info("Train {} ({}) is making an overnight journey, proceeding...", transportLeg.getTrip().getTripShortName(), transportLeg.getRoute().getLongName());
                return Mono.just(transportLeg);
            }
            if ((startTime.equals(LocalTime.MIDNIGHT) || startTime.isAfter(LocalTime.MIDNIGHT))) { // 00:00 - 03:00
                log.info("Train {} ({}) is a night train, proceeding...", transportLeg.getTrip().getTripShortName(), transportLeg.getRoute().getLongName());
                return Mono.just(transportLeg);
            }
            LocalTime now = LocalTime.now();
            if (now.equals(LocalTime.MIDNIGHT) || now.isBefore(THREE_AM)) { // did not manage to record delay for today
                log.error("Delay data is not available for train {} ({}) because it arrived yesterday, aborting...", transportLeg.getTrip().getTripShortName(), transportLeg.getRoute().getLongName());
                return Mono.empty();
            }
        }
        return Mono.just(transportLeg);
    }

    private Mono<Tuple4<LocalTime, LocalTime, String, EmmaShortTimetableResponse.Leg>> extractSchedule(EmmaShortTimetableResponse.Leg entry, String trainNumber) {
        LocalTime arrivalTime = entry.getEndLocalTime();
        LocalTime departureTime = entry.getStartLocalTime();

        if (arrivalTime == null || departureTime == null) {
            log.error("Could not extract scheduled departure and or arrival for train {}", trainNumber);
            log.warn("Assuming train {} has just arrived", trainNumber);
            return Mono.just(Tuples.of(LocalTime.MIDNIGHT, LocalTime.now(), trainNumber, entry));
        }
        return Mono.just(Tuples.of(departureTime, arrivalTime, trainNumber, entry));
    }

    private static Mono<EmmaShortTimetableResponse.Leg> extractTimetableEntry(EmmaShortTimetableResponse response, String trainNumber, LocalDate date) {
        log.info("Extracting timetable entry for train number {} on date {}", trainNumber, date);
        if (response.getPlan() == null ||
                response.getPlan().getItineraries() == null ||
                response.getPlan().getItineraries().isEmpty()) {
            return Mono.error(new ExternalApiFormatMismatchException("Received an empty timetable response", null));
        }

        return response.getPlan().getItineraries().stream()
                .flatMap(itinerary -> itinerary.getLegs().stream())
                .filter(leg -> leg.getRoute() != null && leg.getTrip().getTripShortName() != null
                        && leg.getTrip().getTripShortName().contains(trainNumber))
                .findFirst()
                .map(Mono::just)
                .orElseGet(() -> Mono.error(new TrainNotInServiceException(trainNumber, date)));
    }

    private static Mono<String> extractTrainId(EmmaShortTimetableResponse.Leg timetableEntry, String trainNumber, LocalDate date) {
        log.info("Extracting train id for train number {}", trainNumber);
        var trip = timetableEntry.getTrip();
        if (trip == null) {
            return Mono.error(new TrainNotInServiceException(trainNumber, date));
        }
        return Mono.just(trip.getGtfsId());
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