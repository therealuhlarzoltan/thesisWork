package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Profile("data-source-elvira")
@Component
@Slf4j
@RequiredArgsConstructor
public class ElviraRouteMapper {

    public Mono<List<TrainRouteResponse>> mapToRouteResponse(ElviraTimetableResponse timetableResponse, LocalDate date) {
        List<TrainRouteResponse> routes = new ArrayList<>();
        for (var entry : timetableResponse.getTimetable()) {
            if (entry.getTrainSegments().size() == 1) {
                String scheduledDep = entry.getDetails().getFirst().getDep();
                String scheduledArrival = entry.getTransferStations().getFirst().getScheduledArrival();
                LocalDateTime scheduledDepartureTime = parseTimeSafely(scheduledDep).atDate(date);
                LocalDateTime scheduledArrivalTime = parseTimeSafely(scheduledArrival).atDate(date);
                String actualDep = entry.getDetails().getFirst().getDepReal();
                String actualArrival = entry.getTransferStations().getFirst().getRealArrival();
                LocalDateTime actualDepartureTime = null;
                LocalDateTime actualArrivalTime = null;
                if (actualDep != null && !actualDep.isBlank()) {
                    actualDepartureTime = parseTimeSafely(actualDep).atDate(date);
                    if (actualDepartureTime.isBefore(scheduledDepartureTime)) {
                        Duration duration = Duration.between(actualDepartureTime, scheduledDepartureTime);
                        if (duration.toMinutes() > 4 * 60) {
                            actualDepartureTime = actualDepartureTime.plusDays(1);
                        }
                    }
                }
                if (actualArrival != null && !actualArrival.isBlank() && actualDep != null && !actualDep.isBlank()) {
                    actualArrivalTime = parseTimeSafely(actualArrival).atDate(date);
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
                        LocalDateTime scheduledDepartureTime = parseTimeSafely(scheduledDep).atDate(date);
                        LocalDateTime scheduledArrivalTime = parseTimeSafely(scheduledArrival).atDate(date);
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
                            scheduledDepartureTime = parseTimeSafely(scheduledDep).atDate(date);
                            if (scheduledDepartureTimeRolloverIndex != -1 && i >= scheduledDepartureTimeRolloverIndex) {
                                scheduledDepartureTime = scheduledDepartureTime.plusDays(1);
                            }
                        }
                        if (actualDep != null && !actualDep.isBlank()) {
                            actualDepartureTime = parseTimeSafely(actualDep).atDate(date);
                            if (actualDepartureTimeRolloverIndex != -1 && i >= actualDepartureTimeRolloverIndex) {
                                actualDepartureTime = actualDepartureTime.plusDays(1);
                            }
                        }
                        if (actualArrival != null && !actualArrival.isBlank()) {
                            actualArrivalTime = parseTimeSafely(actualArrival).atDate(date);
                            if (actualArrivalTimeRolloverIndex != -1 && i >= actualArrivalTimeRolloverIndex) {
                                actualArrivalTime = actualArrivalTime.plusDays(1);
                            }
                        }
                        if (scheduledArrival != null && !scheduledArrival.isBlank()) {
                            scheduledArrivalTime = parseTimeSafely(scheduledArrival).atDate(date);
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
                    log.error("An error occurred while processing route", e);
                    continue;
                }
                routes.add(new TrainRouteResponse(trains));
            }
        }
        return Mono.just(routes);
    }

    private <T> int findRolloverIndexForRoutes(List<T> scheduledObjects, LocalTime startTime, Function<T, String> propertyGetter) {
        LocalTime previousTime = startTime;
        for (int i = 0; i < scheduledObjects.size(); i++) {
            T currentStop = scheduledObjects.get(i);
            String timeProperty = propertyGetter.apply(currentStop);
            if (timeProperty != null && !timeProperty.isBlank()) {
                LocalTime currentTime = parseTimeSafely(timeProperty);
                if (currentTime.isBefore(previousTime)) {
                    return i;
                }
                previousTime = parseTimeSafely(timeProperty);
            }
        }
        return -1;
    }

    private static LocalTime parseTimeSafely(String timeStr) {
        return LocalTime.parse(timeStr.equals("24:00") ? "00:00" : timeStr);
    }
}
