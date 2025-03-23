package hu.uni_obuda.thesis.railways.data.raildatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
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
}
