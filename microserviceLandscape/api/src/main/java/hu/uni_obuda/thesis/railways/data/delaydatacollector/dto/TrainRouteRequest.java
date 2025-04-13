package hu.uni_obuda.thesis.railways.data.delaydatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TrainRouteRequest {
    private String trainNumber;
    private String lineNumber;
    private String startStation;
    private String endStation; 
}
