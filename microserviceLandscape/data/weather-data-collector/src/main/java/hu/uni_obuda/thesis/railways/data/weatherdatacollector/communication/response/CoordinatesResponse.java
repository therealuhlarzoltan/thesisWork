package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoordinatesResponse {
    @JsonProperty("lat")
    private double latitude;
    @JsonProperty("long")
    private double longitude;
}
