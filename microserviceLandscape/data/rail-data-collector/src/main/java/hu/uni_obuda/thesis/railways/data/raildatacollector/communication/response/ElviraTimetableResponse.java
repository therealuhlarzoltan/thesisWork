package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ElviraTimetableResponse {

    @JsonProperty("timetable")
    private List<TimetableEntry> timetable;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TimetableEntry {

        @JsonProperty("details")
        private List<JourneyElement> details;

        public List<TrainInfo> getTrainSegments() {
            List<TrainInfo> result = new ArrayList<>();
            for (JourneyElement element : details) {
                if (element.getTrainInfo() != null) {
                    result.add(element.getTrainInfo());
                }
            }
            return result;
        }

        public List<TransferStation> getTransferStations() {
            List<TransferStation> transfers = new ArrayList<>();
            for (JourneyElement element : details) {
                if (element.getTrainInfo() == null && element.getFrom() != null) {
                    transfers.add(new TransferStation(
                            element.getFrom(),
                            element.getDep(),
                            element.getDepReal()
                    ));
                }
            }
            return transfers;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class JourneyElement {

        @JsonProperty("from")
        private String from;

        @JsonProperty("dep")
        private String dep;

        @JsonProperty("dep_real")
        private String depReal;

        @JsonProperty("train_info")
        private TrainInfo trainInfo;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TrainInfo {

        @JsonProperty("url")
        private String url;

        @JsonProperty("get_url")
        private String getUrl;

        @JsonProperty("code")
        private String code;

        @JsonProperty("vsz_code")
        private String vszCode;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TransferStation {

        private String stationName;
        private String scheduledArrival; // from "dep"
        private String realArrival;      // from "dep_real"
    }
}
