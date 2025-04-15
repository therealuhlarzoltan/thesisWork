package hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.DelayEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;

public interface DelayMapper {
    DelayEntity apiToEntity(DelayInfo delayInfo);
    DelayInfo entityToApi(DelayEntity delayEntity);
    DelayEntity addWeatherData(DelayEntity delayEntity, WeatherInfo weatherInfo);
}
