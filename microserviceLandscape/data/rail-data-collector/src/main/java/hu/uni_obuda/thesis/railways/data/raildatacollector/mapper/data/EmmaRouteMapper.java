package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Profile("data-source-emma")
@Component
@Slf4j
@RequiredArgsConstructor
public class EmmaRouteMapper {

    public Mono<List<TrainRouteResponse>> mapToRouteResponse(EmmaTimetableResponse timetableResponse, LocalDateTime now) {
        List<TrainRouteResponse> routes = new ArrayList<>();
        for (var itinerary : timetableResponse.getPlan().getItineraries()) {
            List<TrainRouteResponse.Train> trains = new ArrayList<>();
            for (var leg : itinerary.getLegs()) {
                TrainRouteResponse.Train train = new TrainRouteResponse.Train();
                train.setTrainNumber(leg.getTrip().getTripShortName());
                train.setLineNumber(leg.getRoute().getLongName());
                train.setFromStation(leg.getFrom().getName());
                train.setFromTimeScheduled(extractScheduledFrom(leg, now));
                train.setFromTimeActual(extractActualFrom(leg, now));
                train.setToStation(leg.getTo().getName());
                train.setToTimeScheduled(extractScheduledTo(leg, now));
                train.setToTimeActual(extractActualTo(leg, now));
                trains.add(train);
            }
            routes.add(new TrainRouteResponse(trains));
        }
        return Mono.just(routes);
    }

    private String extractScheduledFrom(EmmaTimetableResponse.Leg leg, LocalDateTime now) {
        LocalTime fromTime = getTimeFromEpochMillis(leg.getStartTime());
        return getDateWithFallback(leg.getServiceDate(), now.toLocalDate()).atTime(fromTime).toString();
    }

    private String extractActualFrom(EmmaTimetableResponse.Leg leg, LocalDateTime now) {
        LocalTime scheduledFromTime = getTimeFromEpochMillis(leg.getStartTime());
        LocalDateTime scheduledFromDateTime = getDateWithFallback(leg.getServiceDate(), now.toLocalDate()).atTime(scheduledFromTime);
        if (scheduledFromDateTime.isAfter(now)) {
            return null;
        }
        return scheduledFromDateTime.plusMinutes(getMinutesFromSeconds(leg.getDepartureDelay())).toString();
    }

    private String extractScheduledTo(EmmaTimetableResponse.Leg leg, LocalDateTime now) {
        LocalTime scheduledToTime = getTimeFromEpochMillis(leg.getEndTime());
        return getDateWithFallback(leg.getServiceDate(), now.toLocalDate()).atTime(scheduledToTime).toString();
    }

    private String extractActualTo(EmmaTimetableResponse.Leg leg, LocalDateTime now) {
        if (extractActualFrom(leg, now) == null) {
            return null;
        }
        LocalTime scheduledToTime = getTimeFromEpochMillis(leg.getEndTime());
        LocalDateTime scheduledToDateTime = getDateWithFallback(leg.getServiceDate(), now.toLocalDate()).atTime(scheduledToTime);
        return scheduledToDateTime.plusMinutes(getMinutesFromSeconds(leg.getArrivalDelay())).toString();
    }

    private LocalDate getDateWithFallback(String dateString, LocalDate fallback) {
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (NullPointerException | DateTimeException e) {
            log.error("Error parsing date: {}, returning fallback {}", dateString, fallback, e);
            return fallback;
        }
    }

    private LocalTime getTimeFromEpochMillis(long epochMillis) {
        return LocalTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private int getMinutesFromSeconds(int seconds) {
        return seconds / 60;
    }
}
