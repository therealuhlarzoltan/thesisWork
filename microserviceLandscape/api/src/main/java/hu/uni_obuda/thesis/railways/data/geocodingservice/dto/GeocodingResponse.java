package hu.uni_obuda.thesis.railways.data.geocodingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class GeocodingResponse {

    private Double latitude;
    private Double longitude;
    private String address;

    @JsonIgnore
    public boolean isEmpty() {
        return latitude == null && longitude == null;
    }
}
