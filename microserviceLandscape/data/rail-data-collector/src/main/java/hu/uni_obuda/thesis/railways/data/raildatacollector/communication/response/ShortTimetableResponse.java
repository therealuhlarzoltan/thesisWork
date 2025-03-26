package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ShortTimetableResponse {

    @JsonProperty("timetable")
    private List<TimetableEntry> timetable;

    @Data
    public static class TimetableEntry {

        @JsonProperty("details")
        private List<TrainDetail> details;
    }

    @Data
    public static class TrainDetail {

        @JsonProperty("train_info")
        private TrainInfo trainInfo;
    }

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
