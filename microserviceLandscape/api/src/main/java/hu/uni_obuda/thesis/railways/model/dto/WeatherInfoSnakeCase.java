package hu.uni_obuda.thesis.railways.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfoSnakeCase {
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

    public WeatherInfoSnakeCase(WeatherInfo original) {
        this.time = original.getTime();
        this.address = original.getAddress();
        this.latitude = original.getLatitude();
        this.longitude = original.getLongitude();
        this.temperature = original.getTemperature();
        this.relativeHumidity = original.getRelativeHumidity();
        this.windSpeedAt10m = original.getWindSpeedAt10m();
        this.windSpeedAt80m = original.getWindSpeedAt80m();
        this.isSnowing = original.getIsSnowing();
        this.snowFall = original.getSnowFall();
        this.snowDepth = original.getSnowDepth();
        this.isRaining = original.getIsRaining();
        this.precipitation = original.getPrecipitation();
        this.rain = original.getRain();
        this.showers = original.getShowers();
        this.visibilityInMeters = original.getVisibilityInMeters();
        this.cloudCoverPercentage = original.getCloudCoverPercentage();
    }
}
