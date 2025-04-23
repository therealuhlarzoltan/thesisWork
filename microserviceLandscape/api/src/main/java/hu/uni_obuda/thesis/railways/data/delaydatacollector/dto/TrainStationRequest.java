package hu.uni_obuda.thesis.railways.data.delaydatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TrainStationRequest {
    private String stationCode;
    private Double latitude;
    private Double longitude;
}
