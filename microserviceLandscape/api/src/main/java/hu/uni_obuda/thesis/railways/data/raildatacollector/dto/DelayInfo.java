package hu.uni_obuda.thesis.railways.data.raildatacollector.dto;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class DelayInfo {
    private String stationCode;
    private String thirdPartyStationUrl;
    private String officialStationUrl;
    private String trainNumber;
    private String scheduledDeparture;
    private String actualDeparture;
    private String scheduledArrival;
    private String actualArrival;
    private Integer arrivalDelay;
    private Integer departureDelay;
    private LocalDate date;
    private WeatherInfo weatherInfo;
}
