package hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.DelayRecord;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.DelayEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainStationEntity;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelayRecordMapperTest {

    @Test
    void entitiesToApiMapsAllFields() {
        DelayRecordMapper mapper = new DelayRecordMapper() {};

        DelayEntity delayEntity = mock(DelayEntity.class);
        TrainStationEntity stationEntity = mock(TrainStationEntity.class);

        String trainNumber = "IC123";
        String stationCode = "BUD";
        Double latitude = 47.4979;
        Double longitude = 19.0402;
        LocalDateTime scheduledArrival = LocalDateTime.of(2025, 1, 1, 10, 15);
        LocalDateTime scheduledDeparture = LocalDateTime.of(2025, 1, 1, 10, 20);
        LocalDateTime actualArrival = LocalDateTime.of(2025, 1, 1, 10, 25);
        LocalDateTime actualDeparture = LocalDateTime.of(2025, 1, 1, 10, 30);
        Integer arrivalDelay = 10;
        Integer departureDelay = 15;
        LocalDate date = LocalDate.of(2025, 1, 1);
        WeatherInfo weather = new WeatherInfo();

        when(delayEntity.getTrainNumber()).thenReturn(trainNumber);
        when(delayEntity.getStationCode()).thenReturn(stationCode);
        when(delayEntity.getScheduledArrival()).thenReturn(scheduledArrival);
        when(delayEntity.getScheduledDeparture()).thenReturn(scheduledDeparture);
        when(delayEntity.getActualArrival()).thenReturn(actualArrival);
        when(delayEntity.getActualDeparture()).thenReturn(actualDeparture);
        when(delayEntity.getArrivalDelay()).thenReturn(arrivalDelay);
        when(delayEntity.getDepartureDelay()).thenReturn(departureDelay);
        when(delayEntity.getDate()).thenReturn(date);
        when(delayEntity.getWeather()).thenReturn(weather);

        when(stationEntity.getLatitude()).thenReturn(latitude);
        when(stationEntity.getLongitude()).thenReturn(longitude);

        DelayRecord result = mapper.entitiesToApi(delayEntity, stationEntity);

        assertNotNull(result);
        assertEquals(trainNumber, result.getTrainNumber());
        assertEquals(stationCode, result.getStationCode());
        assertEquals(latitude, result.getStationLatitude());
        assertEquals(longitude, result.getStationLongitude());
        assertEquals(scheduledArrival, result.getScheduledArrival());
        assertEquals(scheduledDeparture, result.getScheduledDeparture());
        assertEquals(actualArrival, result.getActualArrival());
        assertEquals(actualDeparture, result.getActualDeparture());
        assertEquals(arrivalDelay, result.getArrivalDelay());
        assertEquals(departureDelay, result.getDepartureDelay());
        assertEquals(date, result.getDate());
        assertSame(weather, result.getWeather());

        verify(delayEntity).getTrainNumber();
        verify(delayEntity).getStationCode();
        verify(delayEntity).getScheduledArrival();
        verify(delayEntity).getScheduledDeparture();
        verify(delayEntity).getActualArrival();
        verify(delayEntity).getActualDeparture();
        verify(delayEntity).getArrivalDelay();
        verify(delayEntity).getDepartureDelay();
        verify(delayEntity).getDate();
        verify(delayEntity).getWeather();
        verify(stationEntity).getLatitude();
        verify(stationEntity).getLongitude();
    }
}
