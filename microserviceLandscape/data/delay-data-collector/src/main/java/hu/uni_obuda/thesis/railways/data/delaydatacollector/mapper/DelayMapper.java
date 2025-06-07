package hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.DelayEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Mapper(componentModel = "spring")
public interface DelayMapper {

    static final int EARLY_ARRIVAL_THRESHOLD_HOURS = 12;

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "weather", ignore = true),

    })
    DelayEntity apiToEntity(DelayInfo delayInfo);

    @Mappings({
            @Mapping(source = "weather", target = "weatherInfo"),
    })
    DelayInfo entityToApi(DelayEntity delayEntity);

    default DelayEntity addWeatherData(DelayEntity delayEntity, WeatherInfo weatherInfo) {
        delayEntity.setWeather(weatherInfo);
        return delayEntity;
    }
}
