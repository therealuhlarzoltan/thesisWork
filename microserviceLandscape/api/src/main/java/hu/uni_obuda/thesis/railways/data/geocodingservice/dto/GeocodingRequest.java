package hu.uni_obuda.thesis.railways.data.geocodingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GeocodingRequest {
    private String address;
}
