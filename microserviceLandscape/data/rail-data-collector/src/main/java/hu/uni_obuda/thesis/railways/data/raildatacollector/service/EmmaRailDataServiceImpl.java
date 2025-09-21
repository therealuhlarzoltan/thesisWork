package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway.RailDelayGateway;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.*;
import hu.uni_obuda.thesis.railways.data.raildatacollector.components.EmmaTimetableCache;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

@Profile("data-source-emma")
@Service
@Slf4j
@RequiredArgsConstructor
public class EmmaRailDataServiceImpl implements EmmaRailDataService {

    private static final Map<Character, Character> stationCodeMapping = Map.of(
            'ő', 'õ',
            'ű', 'û'
    );

    private final RailDelayGateway gateway;
    private final EmmaTimetableCache timetableCache;

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
                .flatMap(entry -> extractTrainId(entry, trainNumber, date))
                .flatMap(trainId -> gateway.getShortTrainDetails(trainId, date))
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(shortTrainDetailsResponse ->  mapToDelayInfo(shortTrainDetailsResponse, trainNumber, date))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<TrainRouteResponse> planRoute(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return Flux.error(new UnsupportedOperationException("Not implemented yet"));
    }


    private Mono<EmmaShortTimetableResponse.Leg> checkSchedule(Tuple4<LocalTime, LocalTime, String, EmmaShortTimetableResponse.Leg> schedule) {
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
        log.info("Extracting train URI for train number {}", trainNumber);
        var trip = timetableEntry.getTrip();
        if (trip == null) {
            return Mono.error(new TrainNotInServiceException(trainNumber, date));
        }
        return Mono.just(trip.getGtfsId());
    }

    private Mono<List<DelayInfo>> mapToDelayInfo(EmmaShortTrainDetailsResponse response, String trainNumber, LocalDate date) {
        if (!response.hasArrived()) {
            log.warn("Returning empty station list because train {} hasn't arrived yet", trainNumber);
            return Mono.just(Collections.emptyList());
        }
        if (response.isCancelled()) {
            log.warn("Train {} has been cancelled", trainNumber);
            return Mono.error(new TrainNotInServiceException(trainNumber, date));
        }
        LocalTime localStartTime = toLocalTime(response.getTrip().getStoptimes().getFirst().getScheduledDeparture());
        List<DelayInfo> delayInfos = new ArrayList<>();

        int scheduledDepartureRollover = findRolloverIndex(response.getTrip().getStoptimes(), localStartTime, station -> station.getScheduledDeparture());
        int scheduledArrivalRollover = findRolloverIndex(response.getTrip().getStoptimes(), localStartTime, station -> station.getScheduledArrival());
        int realDepartureRollover = findRolloverIndex(response.getTrip().getStoptimes(), localStartTime, station -> station.getRealtimeDeparture());
        int realArrivalRollover = findRolloverIndex(response.getTrip().getStoptimes(), localStartTime, station -> station.getRealtimeArrival());

        for (int i = 0; i < response.getTrip().getStoptimes().size(); i++) {
            EmmaShortTrainDetailsResponse.StopTime currentStation = response.getTrip().getStoptimes().get(i);
            DelayInfo delayInfo = DelayInfo.builder()
                    .stationCode(adjustStationCodeFormat(currentStation.getStop().getName()))
                    .thirdPartyStationUrl("")
                    .officialStationUrl("")
                    .trainNumber(trainNumber)
                    .date(date)
                    .build();


            if (currentStation.getScheduledArrival() != null) {
                if (scheduledArrivalRollover != -1 && i >= scheduledArrivalRollover) {
                    delayInfo.setScheduledArrival(toLocalTime(currentStation.getScheduledArrival()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setScheduledArrival(toLocalTime(currentStation.getScheduledArrival()).atDate(date).toString());
                }
            }

            if (currentStation.getScheduledDeparture() != null) {
                if (scheduledDepartureRollover != -1 && i >= scheduledDepartureRollover) {
                    delayInfo.setScheduledDeparture(toLocalTime(currentStation.getScheduledDeparture()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setScheduledDeparture(toLocalTime(currentStation.getScheduledDeparture()).atDate(date).toString());
                }
            }

            if (currentStation.getScheduledArrival() != null) {
                if (realArrivalRollover != -1 && i >= realArrivalRollover) {
                    delayInfo.setActualArrival((toLocalTime(currentStation.getRealtimeArrival())).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setActualArrival(toLocalTime(currentStation.getRealtimeArrival()).atDate(date).toString());
                }
            }

            if (currentStation.getRealtimeDeparture() != null) {
                if (realDepartureRollover != -1 && i >= realDepartureRollover) {
                    delayInfo.setActualDeparture(toLocalTime(currentStation.getRealtimeDeparture()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setActualDeparture(toLocalTime(currentStation.getRealtimeDeparture()).atDate(date).toString());
                }
            }

            if (i == 0) {
                delayInfo.setActualArrival(null);
                delayInfo.setScheduledArrival(null);
            }

            if (i == response.getTrip().getStoptimes().size()- 1) {
                delayInfo.setScheduledDeparture(null);
                delayInfo.setActualDeparture(null);
            }

            delayInfo.setArrivalDelay(calculateDelay(delayInfo.getScheduledArrival(), delayInfo.getActualArrival()));
            delayInfo.setDepartureDelay(calculateDelay(delayInfo.getScheduledDeparture(), delayInfo.getActualDeparture()));

            delayInfos.add(delayInfo);
        }
        return Mono.just(delayInfos);
    }

    private int findRolloverIndex(List<EmmaShortTrainDetailsResponse.StopTime> stations, LocalTime startTime, Function<EmmaShortTrainDetailsResponse.StopTime, Integer> epochSecondsGetter) {
        LocalTime previousTime = startTime;
        for (int i = 0; i < stations.size(); i++) {
            EmmaShortTrainDetailsResponse.StopTime currentStation = stations.get(i);
            Integer timeProperty = epochSecondsGetter.apply(currentStation);
            if (timeProperty != null) {
                LocalTime currentTime = toLocalTime(timeProperty);
                if (currentTime.isBefore(previousTime)) {
                    return i;
                }
                previousTime = toLocalTime(timeProperty);
            }
        }
        return -1;
    }

    private <T> int findRolloverIndexForRoutes(List<T> scheduledObjects, LocalTime startTime, Function<T, Integer> epochSecondsGetter) {
        LocalTime previousTime = startTime;
        for (int i = 0; i < scheduledObjects.size(); i++) {
            T currentStop = scheduledObjects.get(i);
            Integer timeProperty = epochSecondsGetter.apply(currentStop);
            if (timeProperty != null) {
                LocalTime currentTime = toLocalTime(timeProperty);
                if (currentTime.isBefore(previousTime)) {
                    return i;
                }
                previousTime = toLocalTime(timeProperty);
            }
        }
        return -1;
    }

    private static LocalTime parseTimeSafe(String timeStr) {
        return LocalTime.parse(timeStr.equals("24:00") ? "00:00" : timeStr);
    }

    private static LocalTime toLocalTime(Integer seconds) {
        return seconds != null ? LocalTime.ofSecondOfDay(seconds) : null;
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

    private String adjustStationCodeFormat(@NonNull String stationCode) {
        for (int i = 0; i < stationCode.length(); i++) {
            if (stationCodeMapping.containsKey(stationCode.charAt(i))) {
                stationCode = stationCode.replace(stationCode.charAt(i), stationCodeMapping.get(stationCode.charAt(i)));
            }
        }
        return stationCode;
    }
}
