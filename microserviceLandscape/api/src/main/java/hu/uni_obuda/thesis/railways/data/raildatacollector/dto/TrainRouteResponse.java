package hu.uni_obuda.thesis.railways.data.raildatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class TrainRouteResponse {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class Station {
        private String stationName;
        private String scheduledTime;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class Train {
        private String trainNumber;
        private String lineNumber;
    }
}
