package hu.uni_obuda.thesis.railways.data.geocodingservice.communication.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CoordinatesResponse {

    private List<Result> results = new ArrayList<>();

    public Double getLatitude() {
        return !results.isEmpty() ? results.getFirst().getGeometry().getLocation().getLat() : null;
    }

    public Double getLongitude() {
        return !results.isEmpty() ? results.getFirst().getGeometry().getLocation().getLng() : null;
    }

    public boolean isPresent() {
        return Objects.nonNull(getLatitude()) && Objects.nonNull(getLongitude());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Result {
        private Geometry geometry;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Geometry {
        private Location location;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Location {
        private double lat;
        private double lng;
    }
}
