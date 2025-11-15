package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

@Profile("data-source-elvira")
@Component
@Slf4j
@RequiredArgsConstructor
public class ElviraDelayMapper {

    public Mono<List<DelayInfo>> mapToDelayInfo(ElviraShortTrainDetailsResponse response, String trainNumber, LocalDate date) {
        if (response.getStations().getLast().getRealArrival() == null || response.getStations().getLast().getRealArrival().isBlank()) {
            log.warn("Returning empty station list because train {} hasn't arrived yet", trainNumber);
            return Mono.just(Collections.emptyList());
        }
        String startTime = response.getStations().getFirst().getScheduledDeparture();
        LocalTime localStartTime;
        try {
            localStartTime = LocalTime.parse(startTime);
        } catch (DateTimeParseException e) {
            log.error("Could not parse scheduled departure at first stop for train {}", trainNumber);
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
                    delayInfo.setScheduledArrival(parseTimeSafely(currentStation.getScheduledArrival()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setScheduledArrival(parseTimeSafely(currentStation.getScheduledArrival()).atDate(date).toString());
                }
            }

            if (currentStation.getScheduledDeparture() != null && !currentStation.getScheduledDeparture().isEmpty()) {
                if (scheduledDepartureRollover != -1 && i >= scheduledDepartureRollover) {
                    delayInfo.setScheduledDeparture(parseTimeSafely(currentStation.getScheduledDeparture()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setScheduledDeparture(parseTimeSafely(currentStation.getScheduledDeparture()).atDate(date).toString());
                }
            }

            if (currentStation.getRealArrival() != null && !currentStation.getRealArrival().isEmpty()) {
                if (realArrivalRollover != -1 && i >= realArrivalRollover) {
                    delayInfo.setActualArrival((parseTimeSafely(currentStation.getRealArrival()).atDate(date).plusDays(1).toString()));
                } else {
                    delayInfo.setActualArrival(parseTimeSafely(currentStation.getRealArrival()).atDate(date).toString());
                }
            }

            if (currentStation.getRealDeparture() != null && !currentStation.getRealDeparture().isEmpty()) {
                if (realDepartureRollover != -1 && i >= realDepartureRollover) {
                    delayInfo.setActualDeparture(parseTimeSafely(currentStation.getRealDeparture()).atDate(date).plusDays(1).toString());
                } else {
                    delayInfo.setActualDeparture(parseTimeSafely(currentStation.getRealDeparture()).atDate(date).toString());
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
                LocalTime currentTime = parseTimeSafely(timeProperty);
                if (currentTime.isBefore(previousTime)) {
                    return i;
                }
                previousTime = parseTimeSafely(timeProperty);
            }
        }
        return -1;
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

    private static LocalTime parseTimeSafely(String timeStr) {
        return LocalTime.parse(timeStr.equals("24:00") ? "00:00" : timeStr);
    }
}
