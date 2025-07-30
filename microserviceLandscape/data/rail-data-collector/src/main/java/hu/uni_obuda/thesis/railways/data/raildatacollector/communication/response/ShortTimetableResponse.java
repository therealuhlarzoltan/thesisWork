package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ShortTimetableResponse {

    @JsonProperty("timetable")
    private List<TimetableEntry> timetable;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TimetableEntry {

        @JsonProperty("starttime")
        private String startTime;
        @JsonProperty("destinationtime")
        private String destinationTime;
        @JsonProperty("details")
        private List<TrainDetail> details;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TrainDetail {

        @JsonProperty("train_info")
        private TrainInfo trainInfo;
    }

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

    public void removeUnnecessaryData() {
        for (var entry : timetable) {
            if (entry.getDetails().size() > 1) {
                entry.getDetails().retainAll(List.of(entry.getDetails().getFirst()));
            }
        }
    }
}
