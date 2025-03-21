package hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WeatherInfo {
    private ZonedDateTime time;
    private int temperature;
    private int humidity;
    private double windSpeed;
    private boolean isSnowing;
    private boolean isRaining;
}
