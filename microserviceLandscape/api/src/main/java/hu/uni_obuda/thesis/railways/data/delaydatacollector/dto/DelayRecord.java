package hu.uni_obuda.thesis.railways.data.delaydatacollector.dto;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DelayRecord {
    private String stationCode;
    private Double stationLatitude;
    private Double stationLongitude;
    private String trainNumber;
    private LocalDateTime scheduledArrival;
    private LocalDateTime scheduledDeparture;
    private LocalDateTime actualArrival;
    private LocalDateTime actualDeparture;
    private Integer arrivalDelay;
    private Integer departureDelay;
    private LocalDate date;
    private WeatherInfo weather;

}
