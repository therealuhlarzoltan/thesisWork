package hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class WeatherInfoRequest {
    private String stationName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime time;
}
