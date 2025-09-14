package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQlShortTimetableResponse {
    private Plan data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Plan {
        private Itineraries plan;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Itineraries {
        private List<Itinerary> itineraries = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Itinerary {
        private Integer numberOfTransfers;
        private List<Leg> legs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {
        private Long startTime; // epoch millis
        private Long endTime;   // epoch millis
        private String headsign; // nullable
        private String mode;     // e.g., "RAIL", "WALK"
        private Route route;     // nullable for WALK
        private Trip trip;       // nullable for WALK


        public LocalTime getStartLocalTime() {
            return startTime != null ? LocalTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()) : null;
        }
        public LocalTime getEndLocalTime()   {
            return endTime != null ? LocalTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault()) : null;
        }
        public boolean isTransitLeg()    { return trip != null || route != null; }
        public long getDurationSeconds() {
            return (startTime != null && endTime != null) ? (endTime - startTime) / 1000 : -1;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private String longName; // e.g., "S70"
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Trip {
        private String tripShortName;  // "2410  személyvonat"
        private String tripHeadsign;   // "Vác"
        private String gtfsId;         // "1:26892408"
        private String id;             // opaque GraphQL id, e.g. "VHJpcDoxOjI2ODkyNDA4"
    }

    public void removeUnnecessaryData() {
        for (Itinerary itinerary : data.getPlan().getItineraries()) {
            itinerary.getLegs().removeIf(leg -> !"RAIL".equalsIgnoreCase(leg.getMode()));
        }

        data.getPlan().getItineraries().removeIf(itinerary -> itinerary.getLegs().isEmpty());
    }
}
