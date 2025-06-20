package hu.uni_obuda.thesis.railways.data.delaydatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class TrainStationResponse {
    private String stationCode;
    private Double latitude;
    private Double longitude;
}
