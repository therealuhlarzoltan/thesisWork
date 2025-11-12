package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.TrainNotInServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static hu.uni_obuda.thesis.railways.data.raildatacollector.util.constant.Constants.STATION_CODE_MAPPING;

@Profile("data-source-emma")
@Component
@Slf4j
@RequiredArgsConstructor
public class EmmaDelayMapper {

    public Mono<List<DelayInfo>> mapToDelayInfo(EmmaShortTrainDetailsResponse response, String trainNumber, LocalDate date) {
        LocalTime now = LocalTime.now();
        boolean skipArrivalCheck = !now.isBefore(LocalTime.of(23, 55));
        if (skipArrivalCheck) {
            log.info("Not checking arrival for train {} to avoid date overflow", trainNumber);
        } else {
            if (!response.hasArrived()) {
                log.warn("Returning empty station list because train {} hasn't arrived yet", trainNumber);
                return Mono.just(Collections.emptyList());
            }
        }

        if (response.isCancelled()) {
            log.warn("Train {} has been cancelled", trainNumber);
            return Mono.error(new TrainNotInServiceException(trainNumber, date));
        }
        LocalDateTime operationDayMidnight = date.atStartOfDay();
        List<DelayInfo> delayInfos = new ArrayList<>();

        for (int i = 0; i < response.getTrip().getStoptimes().size(); i++) {
            EmmaShortTrainDetailsResponse.StopTime currentStation = response.getTrip().getStoptimes().get(i);
            DelayInfo delayInfo = DelayInfo.builder()
                    .stationCode(adjustStationCodeFormat(currentStation.getStop().getName()))
                    .thirdPartyStationUrl("")
                    .officialStationUrl("")
                    .trainNumber(trainNumber)
                    .date(date)
                    .build();


            if (currentStation.getScheduledArrival() != null && i != 0) {
                LocalDateTime scheduledArrival = operationDayMidnight.plusSeconds(currentStation.getScheduledArrival());
                delayInfo.setScheduledArrival(scheduledArrival.toString());
            }

            if (currentStation.getScheduledDeparture() != null && i != response.getTrip().getStoptimes().size() - 1) {
                LocalDateTime scheduledDeparture = operationDayMidnight.plusSeconds(currentStation.getScheduledDeparture());
                delayInfo.setScheduledDeparture(scheduledDeparture.toString());
            }

            if (currentStation.getRealtimeArrival() != null && i != 0) {
                LocalDateTime realtimeArrival = operationDayMidnight.plusSeconds(currentStation.getRealtimeArrival());
                delayInfo.setActualArrival(realtimeArrival.toString());
                delayInfo.setArrivalDelay(calculateDelay(currentStation.getArrivalDelay()));
            }

            if (currentStation.getRealtimeDeparture() != null && i != response.getTrip().getStoptimes().size() - 1) {
                LocalDateTime realtimeDeparture = operationDayMidnight.plusSeconds(currentStation.getRealtimeDeparture());
                delayInfo.setActualDeparture(realtimeDeparture.toString());
                delayInfo.setDepartureDelay(calculateDelay(currentStation.getDepartureDelay()));
            }

            delayInfos.add(delayInfo);
        }
        return Mono.just(delayInfos);
    }

    private Integer calculateDelay(Integer delayInSeconds) {
        if (delayInSeconds == null) {
            return null;
        }
        return delayInSeconds / 60;
    }

    private String adjustStationCodeFormat(@NonNull String stationCode) {
        for (int i = 0; i < stationCode.length(); i++) {
            if (STATION_CODE_MAPPING.containsKey(stationCode.charAt(i))) {
                stationCode = stationCode.replace(stationCode.charAt(i), STATION_CODE_MAPPING.get(stationCode.charAt(i)));
            }
        }
        return stationCode;
    }
}
