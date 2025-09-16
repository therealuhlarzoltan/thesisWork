package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQlShortTrainDetailsResponse {
    private Trip trip;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Trip {
        private String id;
        private Route route;
        private String tripShortName;
        private String tripHeadsign;
        private String serviceId;
        private List<StopTime> stoptimes;
        private List<VehiclePosition> vehiclePositions;

        @JsonIgnore
        public boolean hasVehiclePositions() {
           return vehiclePositions != null && !vehiclePositions.isEmpty();
        }

        @JsonIgnore
        public boolean hasInconsistentStopTimes() {
            int previousArrival = Integer.MIN_VALUE;
            int previousDeparture = Integer.MIN_VALUE;
            for (StopTime stopTime : stoptimes) {
                int arrival = stopTime.getRealtimeArrival() != null
                        ? stopTime.getRealtimeArrival()
                        : (stopTime.getScheduledArrival() != null ? stopTime.getScheduledArrival() : Integer.MIN_VALUE);

                int departure = stopTime.getRealtimeDeparture() != null
                        ? stopTime.getRealtimeDeparture()
                        : (stopTime.getScheduledDeparture() != null ? stopTime.getScheduledDeparture() : Integer.MIN_VALUE);
                if (arrival < previousArrival || departure < previousDeparture) {
                    return true;
                }
                previousArrival = stopTime.realtimeArrival;
                previousDeparture = stopTime.realtimeDeparture;
            }
            return false;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private String id;
        private String mode;
        private String longName;
        private Integer type;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StopTime {
        private Integer scheduledArrival;
        private Integer realtimeArrival;
        private Integer arrivalDelay;
        private Integer scheduledDeparture;
        private Integer realtimeDeparture;
        private Integer departureDelay;
        private Long serviceDay;
        private Stop stop;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stop {
        private String id;
        private String stopId;
        private String name;
        private Double lat;
        private Double lon;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VehiclePosition {
        private Double lat;
        private Double lon;
    }

    @JsonIgnore
    public boolean hasArrived() {
        return !trip.hasVehiclePositions();
    }

    @JsonIgnore
    public boolean isCancelled() {
        return trip.hasInconsistentStopTimes();
    }
}
