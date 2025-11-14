package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.*;
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
                train.setFromTimeScheduled(extractScheduledFrom(leg));
                train.setFromTimeActual(extractActualFrom(leg, now.toLocalTime()));
                train.setToStation(leg.getTo().getName());
                train.setToTimeScheduled(extractScheduledTo(leg));
                train.setToTimeActual(extractActualTo(leg, now.toLocalTime()));
                trains.add(train);
            }
            routes.add(new TrainRouteResponse(trains));
        }
        return Mono.just(routes);
    }

    private String extractScheduledFrom(EmmaTimetableResponse.Leg leg) {
        return getTimeFromEpochMillis(leg.getStartTime()).toString();
    }

    private String extractActualFrom(EmmaTimetableResponse.Leg leg, LocalTime now) {
        LocalTime scheduledFrom = getTimeFromEpochMillis(leg.getStartTime());
        if (scheduledFrom.isBefore(now)) {
            return null;
        }
        return scheduledFrom.plusMinutes(getMinutesFromSeconds(leg.getDepartureDelay())) .toString();
    }

    private String extractScheduledTo(EmmaTimetableResponse.Leg leg) {
        return getTimeFromEpochMillis(leg.getEndTime()).toString();
    }

    private String extractActualTo(EmmaTimetableResponse.Leg leg, LocalTime now) {
        LocalTime scheduledTo = getTimeFromEpochMillis(leg.getEndTime());
        if (scheduledTo.isBefore(now)) {
            return null;
        }
        return scheduledTo.plusMinutes(getMinutesFromSeconds(leg.getArrivalDelay())).toString();
    }

    private LocalTime getTimeFromEpochMillis(long epochMillis) {
        return LocalTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private int getMinutesFromSeconds(int seconds) {
        return seconds / 60;
    }
}
