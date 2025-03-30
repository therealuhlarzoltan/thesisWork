package hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class WeatherInfo {
    private LocalDateTime time;
    private String address;
    private Double latitude;
    private Double longitude;

    private Double temperature;
    private Double relativeHumidity;

    private Double windSpeedAt10m;
    private Double windSpeedAt80m;

    private Boolean isSnowing;
    private Double snowFall;
    private Double snowDepth;

    private Boolean isRaining;
    private Double precipitation;
    private Double rain;
    private Double showers;

    private Integer visibilityInMeters;
    private Integer cloudCoverPercentage;

}
