package hu.uni_obuda.thesis.railways.route.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RouteResponse {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Train {
        private String trainNumber;
        private String lineNumber;
        private String fromStation;
        private String toStation;
        private String fromTimeScheduled;
        private String toTimeScheduled;
        private String  fromTimeExpected;
        private String toTimeExpected;
    }

    private List<Train> trains;

}
