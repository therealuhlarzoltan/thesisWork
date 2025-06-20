package hu.uni_obuda.thesis.railways.route.routeplannerservice.helper;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

@Slf4j
@RequiredArgsConstructor
@Component
public class TimetableProcessingHelper {

    private static final BiPredicate<TrainRouteResponse, LocalDateTime> DEPARTURE_TIME_PREDICATE = (trains, start) -> start.isBefore(Objects.requireNonNullElse(getStartTime(trains), LocalDateTime.MIN));
    private static final BiPredicate<TrainRouteResponse, LocalDateTime> ARRIVAL_TIME_PREDICATE = (trains, end) -> end.isAfter(Objects.requireNonNullElse(getEndTime(trains), LocalDateTime.MAX));
    private static final BiPredicate<TrainRouteResponse, Integer> CHANGES_PREDICATE = (trains, changes) -> changes + 1 >= trains.getTrains().size();

    public Flux<TrainRouteResponse> filterByDeparture(LocalDateTime departureTime, Flux<TrainRouteResponse> trainRouteResponses) {
        return trainRouteResponses
                .filter(Objects::nonNull)
                .filter(route -> DEPARTURE_TIME_PREDICATE.test(route, departureTime));
    }

    public Flux<TrainRouteResponse> filterByArrival(LocalDateTime arrivalTime, Flux<TrainRouteResponse> trainRouteResponses) {
        return trainRouteResponses
                .filter(Objects::nonNull)
                .filter(route -> ARRIVAL_TIME_PREDICATE.test(route, arrivalTime));
    }

    public Flux<TrainRouteResponse> filterByChanges(int numberOfChanges, Flux<TrainRouteResponse> trainRouteResponses) {
        return trainRouteResponses
                .filter(Objects::nonNull)
                .filter(route -> CHANGES_PREDICATE.test(route, numberOfChanges));
    }

    private static LocalDateTime parseSafe(String dateString) {
        if (dateString == null ) {
            log.error("Date string is null");
            return null;
        }
        try {
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            log.error("Could not parse date: {}", dateString);
            return null;
        }
    }

    private static LocalDateTime getStartTime(TrainRouteResponse trainRouteResponse) {
        var train = trainRouteResponse.getTrains().stream().filter(Objects::nonNull).findFirst();
        return parseSafe(train.map(t -> Objects.requireNonNullElse(t.getFromTimeScheduled(), "")).orElseGet(String::new));
    }

    private static LocalDateTime getEndTime(TrainRouteResponse trainRouteResponse) {
        var trainList = trainRouteResponse.getTrains().stream().filter(Objects::nonNull).toList();
        var train = Optional.ofNullable(!trainList.isEmpty() ? trainList.getLast() : null);
        return parseSafe(train.map(t -> Objects.requireNonNullElse(t.getToTimeScheduled(), "")).orElseGet(String::new));
    }

}
