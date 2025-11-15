package hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.DelayRecord;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.DelayEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainStationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DelayRecordMapper {
    default DelayRecord entitiesToApi(DelayEntity delayEntity, TrainStationEntity stationEntity) {
        return DelayRecord.builder()
                .trainNumber(delayEntity.getTrainNumber())
                .stationCode(delayEntity.getStationCode())
                .stationLatitude(stationEntity.getLatitude())
                .stationLongitude(stationEntity.getLongitude())
                .scheduledArrival(delayEntity.getScheduledArrival())
                .scheduledDeparture(delayEntity.getScheduledDeparture())
                .actualArrival(delayEntity.getActualArrival())
                .actualDeparture(delayEntity.getActualDeparture())
                .arrivalDelay(delayEntity.getArrivalDelay())
                .departureDelay(delayEntity.getDepartureDelay())
                .date(delayEntity.getDate())
                .weather(delayEntity.getWeather())
                .build();
    }
}
