package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StationDelay {
    private String stationCode;
    private String thirdPartyStationUrl;
    private String officialStationUrl;
    private Integer scheduledDeparture;
    private Integer actualDeparture;
    private Integer scheduledArrival;
    private Integer actualArrival;
}
