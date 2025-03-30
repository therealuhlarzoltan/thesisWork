package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CoordinatesResponse {
    @JsonProperty("lat")
    private double latitude;
    @JsonProperty("long")
    private double longitude;
}
