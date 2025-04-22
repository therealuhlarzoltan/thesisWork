package hu.uni_obuda.thesis.railways.data.geocodingservice.dto;

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
}
