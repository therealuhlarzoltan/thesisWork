package hu.uni_obuda.thesis.railways.data.raildatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
        private LocalDateTime scheduledArrivalTime;
        private LocalDateTime actualArrivalTime;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class Train {
        private String trainNumber;
        private String lineNumber;
        private String from;
        private String to;
        private LocalDateTime scheduledDepartureTime;
        private LocalDateTime actualDepartureTime;
    }
}
