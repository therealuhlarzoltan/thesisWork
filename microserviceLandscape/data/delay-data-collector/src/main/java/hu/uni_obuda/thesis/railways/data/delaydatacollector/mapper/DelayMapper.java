package hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.DelayEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Mapper(componentModel = "spring")
public interface DelayMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "weather", ignore = true),
            @Mapping(target = "scheduledDeparture", expression = "java(toLocalDateTime(delayInfo.getScheduledDeparture(), delayInfo.getDate()))"),
            @Mapping(target = "actualDeparture", expression = "java(toLocalDateTime(delayInfo.getActualDeparture(), delayInfo.getDate()))"),
            @Mapping(target = "scheduledArrival", expression = "java(toLocalDateTime(delayInfo.getScheduledArrival(), delayInfo.getDate()))"),
            @Mapping(target = "actualArrival", expression = "java(toLocalDateTime(delayInfo.getActualArrival(), delayInfo.getDate()))")
    })
    DelayEntity apiToEntity(DelayInfo delayInfo);

    @Mappings({
            @Mapping(source = "weather", target = "weatherInfo"),
            @Mapping(target = "scheduledDeparture", expression = "java(toTimeString(delayEntity.getScheduledDeparture()))"),
            @Mapping(target = "actualDeparture", expression = "java(toTimeString(delayEntity.getActualDeparture()))"),
            @Mapping(target = "scheduledArrival", expression = "java(toTimeString(delayEntity.getScheduledArrival()))"),
            @Mapping(target = "actualArrival", expression = "java(toTimeString(delayEntity.getActualArrival()))")
    })
    DelayInfo entityToApi(DelayEntity delayEntity);

    default DelayEntity addWeatherData(DelayEntity delayEntity, WeatherInfo weatherInfo) {
        delayEntity.setWeather(weatherInfo);
        return delayEntity;
    }

    default LocalDateTime toLocalDateTime(String timeString, LocalDate date) {
        if (timeString == null || timeString.isBlank() || date == null) {
            return null;
        }
        try {
            return LocalDateTime.of(date, LocalTime.parse(timeString));
        } catch (Exception e) {
            return null;
        }
    }

    default String toTimeString(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.toLocalTime().toString();
    }
}
