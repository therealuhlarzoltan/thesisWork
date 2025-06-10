package hu.uni_obuda.thesis.railways.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RouteResponse {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class Train {
        private String trainNumber;
        private String lineNumber;
        private String fromStation;
        private String toStation;
        private String fromTimeScheduled;
        private String toTimeScheduled;
        private String  fromTimeActual;
        private String toTimeActual;
        private String fromTimePredicted;
        private String toTimePredicted;
    }

    private List<Train> trains;

}
