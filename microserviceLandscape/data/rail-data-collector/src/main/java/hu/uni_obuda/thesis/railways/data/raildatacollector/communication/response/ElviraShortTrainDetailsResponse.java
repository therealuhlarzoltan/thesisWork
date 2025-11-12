package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ElviraShortTrainDetailsResponse {

    @JsonProperty("stations")
    private List<Station> stations;

    public static class Station {

        @JsonProperty("station")
        private StationDetails stationDetails;
        @JsonProperty("schedule")
        private Schedule schedule;
        @JsonProperty("real")
        private Real real;
        @JsonProperty("expected")
        private Expected expected;

        public String getCode() {
            return stationDetails != null ? stationDetails.getCode() : null;
        }

        public String getUrl() {
            return stationDetails != null ? stationDetails.getUrl() : null;
        }

        public String getGetUrl() {
            return stationDetails != null ? stationDetails.getGetUrl() : null;
        }

        public String getScheduledDeparture() {
            return schedule != null ? schedule.getDeparture() : null;
        }

        public String getScheduledArrival() {
            return schedule != null ? schedule.getArrival() : null;
        }

        public String getRealDeparture() {
            return real != null ? real.getDeparture() : null;
        }

        public String getRealArrival() {
            return real != null ? real.getArrival() : null;
        }

        public String getExpectedDeparture() {
            return expected != null ? expected.getDeparture() : null;
        }

        public String getExpectedArrival() {
            return expected != null ? expected.getArrival() : null;
        }
    }

    @Data
    public static class StationDetails {

        @JsonProperty("url")
        private String url;
        @JsonProperty("get_url")
        private String getUrl;
        @JsonProperty("code")
        private String code;
    }

    @Data
    public static class Schedule {

        @JsonProperty("departure")
        private String departure;
        @JsonProperty("arrival")
        private String arrival;
    }

    @Data
    public static class Real {

        @JsonProperty("departure")
        private String departure;
        @JsonProperty("arrival")
        private String arrival;
    }

    @Data
    public static class Expected {

        @JsonProperty("departure")
        private String departure;
        @JsonProperty("arrival")
        private String arrival;
    }
}
