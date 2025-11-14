package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmmaTimetableResponse {

    private Plan plan;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Plan {
        private List<Itinerary> itineraries;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Itinerary {
        private long duration;
        private int numberOfTransfers;
        private long endTime;
        private List<Leg> legs;
        private long startTime;
        private long waitingTime;
        private long walkTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {
        private Agency agency;
        private int arrivalDelay;
        private int departureDelay;
        private double distance;
        private double duration;
        private long endTime;
        private Place from;
        private Place to;
        private String headsign;
        private String mode;
        private Route route;
        private long startTime;
        private boolean transitLeg;
        private String serviceDate;
        private Trip trip;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Agency {
        private String name;
        private String timezone;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {
        private double lat;
        private double lon;
        private String name;
        private Stop stop;
        private String vertexType;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stop {
        private String code;
        private String id;
        private String timezone;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private String longName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Trip {
        private String tripShortName;
        private String tripHeadsign;
        private boolean isThroughCoach;

    }

    public void removeUnnecessaryData() {
        plan.getItineraries().forEach(itinerary -> {
            itinerary.getLegs()
                    .removeIf(leg -> "RAIL".equalsIgnoreCase(leg.getMode()) || "RAIL_REPLACEMENT_BUS".equalsIgnoreCase(leg.getMode())
                    || "SUBURBAN_RAIL".equalsIgnoreCase(leg.getMode()) || "TRAMTRAIN".equalsIgnoreCase(leg.getMode()));
        });
        plan.getItineraries().removeIf(itinerary -> itinerary.getLegs().isEmpty());
    }
}
