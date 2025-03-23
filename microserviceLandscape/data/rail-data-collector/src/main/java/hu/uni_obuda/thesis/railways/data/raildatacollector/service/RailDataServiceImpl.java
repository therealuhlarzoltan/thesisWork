package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway.RailDelayGateway;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RailDataServiceImpl implements RailDataService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH::mm");

    private final RailDelayGateway gateway;

    @Override
    public Flux<DelayInfo> getDelayInfo(String trainNumber, String from, String to, LocalDate date) {
        return gateway.getShortTimetable(from, to)
                .flatMap(timetableResponse -> extractTrainUri(timetableResponse, trainNumber))
                .flatMap(trainUri -> gateway.getShortTrainDetails(trainUri))
                .flatMap(shortTrainDetailsResponse ->  mapToDelayInfo(shortTrainDetailsResponse, trainNumber, date))
                .flatMapMany(Flux::fromIterable);
    }

    private static Mono<String> extractTrainUri(ShortTimetableResponse response, String trainNumber) {
        return response.getTimetable().stream()
                .flatMap(entry -> entry.getDetails().stream())
                .filter(details -> details.getTrainInfo().getCode().equals(trainNumber))
                .findFirst()
                .map(details -> Mono.just(details.getTrainInfo().getUrl()))
                .orElse(Mono.error(new InvalidInputDataException("Train number not found")));
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
        if (station.getRealArrival() != null || station.getRealDeparture() != null) {
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

        return new DelayInfo(station.getCode(), station.getGetUrl().split("=")[1], station.getUrl(), trainNumber, station.getScheduledArrival(), station.getRealArrival(), station.getScheduledDeparture(), station.getRealDeparture(), arrivalDelay, departureDelay, date);
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
}
