package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway.ElviraRailDataGateway;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.components.ElviraTimetableCache;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ElviraRailDataServiceImpl implements ElviraRailDataService {

    private static final Logger LOG = LoggerFactory.getLogger(ElviraRailDataServiceImpl.class);

    private final ElviraRailDataGateway gateway;
    private final ElviraTimetableCache timetableCache;

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
                .flatMap(timetableResponse -> extractTimetableEntry(timetableResponse, trainNumber, date))
                .flatMap(entry -> extractSchedule(entry, trainNumber))
                .flatMap(this::checkSchedule)
                .flatMap(entry -> extractTrainUri(entry, trainNumber, date))
                .flatMap(gateway::getShortTrainDetails)
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(shortTrainDetailsResponse ->  mapToDelayInfo(shortTrainDetailsResponse, trainNumber, date))
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<TrainRouteResponse> planRoute(String from, String to, LocalDate date) {
        return gateway.getTimetable(from, to, date)
                .onErrorMap(WebClientResponseException.NotFound.class, this::mapNotFoundToExternalApiException)
                .onErrorMap(WebClientResponseException.BadRequest.class, this::mapBadRequestToExternalApiException)
                .onErrorMap(WebClientRequestException.class, this::mapWebClientRequestExceptionToApiException)
                .flatMap(timetableResponse ->  mapToRouteResponse(timetableResponse, date))
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<ElviraShortTimetableResponse.TimetableEntry> checkSchedule(Tuple4<LocalTime, LocalTime, String, ElviraShortTimetableResponse.TimetableEntry> schedule) {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.MIDNIGHT) && now.isBefore(LocalTime.MIDNIGHT.plusHours(4))) {
            LOG.info("Not checking schedule in the early hours of the day, proceeding...");
            return Mono.just(schedule.getT4());
        }
        if (now.isBefore(schedule.getT1())) {
            LOG.warn("Train {} has not departed yet according to schedule, aborting...", schedule.getT3());
            return Mono.empty();
        } else if (now.isBefore(schedule.getT2())) {
            LOG.warn("Train {} has not arrived yet according to schedule, aborting...", schedule.getT3());
            return Mono.empty();
        } else {
            LOG.info("Train should have arrived by now according to schedule, proceeding...");
            return Mono.just(schedule.getT4());
        }
    }

    private Mono<Tuple4<LocalTime, LocalTime, String, ElviraShortTimetableResponse.TimetableEntry>> extractSchedule(ElviraShortTimetableResponse.TimetableEntry entry, String trainNumber) {
        String departureString = entry.getStartTime();
        String arrivalString = entry.getDestinationTime();
        LocalTime arrivalTime;
        LocalTime departureTime;
        try {
            departureTime = parseTimeSafe(departureString);
            arrivalTime = parseTimeSafe(arrivalString);
        } catch (Exception e) {
            LOG.error("Could not extract scheduled departure and or arrival for train {}", trainNumber, e);
            LOG.warn("Assuming train {} has just arrived", trainNumber);
            return Mono.just(Tuples.of(LocalTime.MIDNIGHT, LocalTime.now(), trainNumber, entry));
        }
        return Mono.just(Tuples.of(departureTime, arrivalTime, trainNumber, entry));
    }

    private static Mono<ElviraShortTimetableResponse.TimetableEntry> extractTimetableEntry(ElviraShortTimetableResponse response, String trainNumber, LocalDate date) {
        LOG.info("Extracting timetable entry for train number {} on date {}", trainNumber, date);
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
        LOG.info("Extracting train URI for train number {}", trainNumber);
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

    private Mono<List<TrainRouteResponse>> mapToRouteResponse(ElviraTimetableResponse timetableResponse, LocalDate date) {
        List<TrainRouteResponse> routes = new ArrayList<>();
        for (var entry : timetableResponse.getTimetable()) {
            if (entry.getTrainSegments().size() == 1) {
                String scheduledDep = entry.getDetails().getFirst().getDep();
                String scheduledArrival = entry.getTransferStations().getFirst().getScheduledArrival();
                LocalDateTime scheduledDepartureTime = parseTimeSafe(scheduledDep).atDate(date);
                LocalDateTime scheduledArrivalTime = parseTimeSafe(scheduledArrival).atDate(date);
                String actualDep = entry.getDetails().getFirst().getDepReal();
                String actualArrival = entry.getTransferStations().getFirst().getRealArrival();
                LocalDateTime actualDepartureTime = null;
                LocalDateTime actualArrivalTime = null;
                if (actualDep != null && !actualDep.isBlank()) {
                    actualDepartureTime = parseTimeSafe(actualDep).atDate(date);
                    if (actualDepartureTime.isBefore(scheduledDepartureTime)) {
                        Duration duration = Duration.between(actualDepartureTime, scheduledDepartureTime);
                        if (duration.toMinutes() > 4 * 60) {
                            actualDepartureTime = actualDepartureTime.plusDays(1);
                        }
                    }
                }
                if (actualArrival != null && !actualArrival.isBlank() && actualDep != null && !actualDep.isBlank()) {
                    actualArrivalTime = parseTimeSafe(actualArrival).atDate(date);
                    if (actualArrivalTime.isBefore(actualDepartureTime)) {
                        actualArrivalTime = actualArrivalTime.plusDays(1);
                    }
                }
                if (scheduledArrivalTime.isBefore(scheduledDepartureTime)) {
                    scheduledArrivalTime = scheduledArrivalTime.plusDays(1);
                }
                var train = TrainRouteResponse.Train.builder()
                        .trainNumber(entry.getTrainSegments().getFirst().getCode())
                        .lineNumber(entry.getTrainSegments().getFirst().getVszCode())
                        .fromStation(entry.getDetails().getFirst().getFrom())
                        .fromTimeScheduled(scheduledDepartureTime.toString())
                        .fromTimeActual(Objects.toString(actualDepartureTime, ""))
                        .toStation(entry.getTransferStations().getFirst().getStationName())
                        .toTimeScheduled(scheduledArrivalTime.toString())
                        .toTimeActual(Objects.toString(actualArrivalTime, ""))
                        .build();
                var route = new TrainRouteResponse(List.of(train));
                routes.add(route);
            } else {
                List<TrainRouteResponse.Train> trains = new ArrayList<>();
                int scheduledDepartureTimeRolloverIndex = findRolloverIndexForRoutes(entry.getDetails(), LocalTime.parse(entry.getDetails().getFirst().getDep()), ElviraTimetableResponse.JourneyElement::getDep);
                int scheduledArrivalRolloverIndex = findRolloverIndexForRoutes(entry.getTransferStations(), LocalTime.parse(entry.getDetails().getFirst().getDep()), ElviraTimetableResponse.TransferStation::getScheduledArrival);
                int actualDepartureTimeRolloverIndex = findRolloverIndexForRoutes(entry.getDetails(), LocalTime.parse(entry.getDetails().getFirst().getDep()), ElviraTimetableResponse.JourneyElement::getDep);
                int actualArrivalTimeRolloverIndex = findRolloverIndexForRoutes(entry.getTransferStations(), LocalTime.parse(entry.getDetails().getFirst().getDep()), ElviraTimetableResponse.TransferStation::getRealArrival);
                try {
                    for (int i = 0; i < entry.getTrainSegments().size(); i++) {
                        String scheduledDep = entry.getDetails().get(i * 2).getDep();
                        String scheduledArrival = entry.getTransferStations().get(i).getScheduledArrival();
                        LocalDateTime scheduledDepartureTime = parseTimeSafe(scheduledDep).atDate(date);
                        LocalDateTime scheduledArrivalTime = parseTimeSafe(scheduledArrival).atDate(date);
                        String actualDep = entry.getDetails().get(i * 2).getDepReal();
                        String actualArrival = entry.getTransferStations().get(i).getRealArrival();
                        LocalDateTime actualDepartureTime = null;
                        LocalDateTime actualArrivalTime = null;
                    /*
                    if (actualDep != null && !actualDep.isBlank()) {
                        actualDepartureTime = parseTimeSafe(actualDep).atDate(date);
                        if (actualDepartureTime.isBefore(scheduledDepartureTime)) {
                            Duration duration = Duration.between(actualDepartureTime, scheduledDepartureTime);
                            if (duration.toMinutes() > 4 * 60) {
                                actualDepartureTime = actualDepartureTime.plusDays(1);
                            }
                        }
                    }
                     */
                        if (scheduledDep != null && !scheduledDep.isBlank()) {
                            scheduledDepartureTime = parseTimeSafe(scheduledDep).atDate(date);
                            if (scheduledDepartureTimeRolloverIndex != -1 && i >= scheduledDepartureTimeRolloverIndex) {
                                scheduledDepartureTime = scheduledDepartureTime.plusDays(1);
                            }
                        }
                        if (actualDep != null && !actualDep.isBlank()) {
                            actualDepartureTime = parseTimeSafe(actualDep).atDate(date);
                            if (actualDepartureTimeRolloverIndex != -1 && i >= actualDepartureTimeRolloverIndex) {
                                actualDepartureTime = actualDepartureTime.plusDays(1);
                            }
                        }
                        if (actualArrival != null && !actualArrival.isBlank()) {
                            actualArrivalTime = parseTimeSafe(actualArrival).atDate(date);
                            if (actualArrivalTimeRolloverIndex != -1 && i >= actualArrivalTimeRolloverIndex) {
                                actualArrivalTime = actualArrivalTime.plusDays(1);
                            }
                        }
                        if (scheduledArrival != null && !scheduledArrival.isBlank()) {
                            scheduledArrivalTime = parseTimeSafe(scheduledArrival).atDate(date);
                            if (scheduledArrivalRolloverIndex != -1 && i >= scheduledArrivalRolloverIndex) {
                                scheduledArrivalTime = scheduledArrivalTime.plusDays(1);
                            }
                        }
                        var train = TrainRouteResponse.Train.builder()
                                .trainNumber(entry.getTrainSegments().get(i).getCode())
                                .lineNumber(entry.getTrainSegments().get(i).getVszCode())
                                .fromStation(entry.getDetails().get(i * 2).getFrom())
                                .fromTimeScheduled(scheduledDepartureTime.toString())
                                .fromTimeActual(Objects.toString(actualDepartureTime, ""))
                                .toStation(entry.getTransferStations().get(i).getStationName())
                                .toTimeScheduled(scheduledArrivalTime.toString())
                                .toTimeActual(Objects.toString(actualArrivalTime, ""))
                                .build();
                        trains.add(train);
                    }
                } catch (Exception e) {
                    LOG.error("An error occurred while processing route", e);
                    continue;
                }
                routes.add(new TrainRouteResponse(trains));
            }
        }
        return Mono.just(routes);
    }

    private Mono<List<DelayInfo>> mapToDelayInfo(ElviraShortTrainDetailsResponse response, String trainNumber, LocalDate date) {
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
            ElviraShortTrainDetailsResponse.Station currentStation = response.getStations().get(i);
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

    private int findRolloverIndex(List<ElviraShortTrainDetailsResponse.Station> stations, LocalTime startTime, Function<ElviraShortTrainDetailsResponse.Station, String> propertyGetter) {
        LocalTime previousTime = startTime;
        for (int i = 0; i < stations.size(); i++) {
            ElviraShortTrainDetailsResponse.Station currentStation = stations.get(i);
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

    private <T> int findRolloverIndexForRoutes(List<T> scheduledObjects, LocalTime startTime, Function<T, String> propertyGetter) {
        LocalTime previousTime = startTime;
        for (int i = 0; i < scheduledObjects.size(); i++) {
            T currentStop = scheduledObjects.get(i);
            String timeProperty = propertyGetter.apply(currentStop);
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
