package hu.uni_obuda.thesis.railways.data.delaydatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TrainRouteResponse {
    private Integer routeId;
    private String trainNumber;
    private String lineNumber;
    private String startStation;
    private String endStation;
}
