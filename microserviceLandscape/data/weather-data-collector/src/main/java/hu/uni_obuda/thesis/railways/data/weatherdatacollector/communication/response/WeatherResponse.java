package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response;

import lombok.AllArgsConstructor;
import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class WeatherResponse {

    private double latitude;
    private double longitude;

    @JsonProperty("generationtime_ms")
    private double generationTimeMs;

    @JsonProperty("utc_offset_seconds")
    private int utcOffsetSeconds;

    private String timezone;

    @JsonProperty("timezone_abbreviation")
    private String timezoneAbbreviation;

    private int elevation;

    @JsonProperty("hourly_units")
    private HourlyUnits hourlyUnits;

    private Hourly hourly;

    private boolean isPresent = true;

    public static class HourlyUnits {
        private String time;

        @JsonProperty("temperature_2m")
        private String temperature2m;

        @JsonProperty("relative_humidity_2m")
        private String relativeHumidity2m;

        @JsonProperty("snow_depth")
        private String snowDepth;

        private String snowfall;
        private String precipitation;
        private String showers;
        private String rain;
        private String visibility;

        @JsonProperty("wind_speed_10m")
        private String windSpeed10m;

        @JsonProperty("cloud_cover")
        private String cloudCover;

        @JsonProperty("wind_speed_80m")
        private String windSpeed80m;
    }

    @Data
    public static class Hourly {
        private List<String> time = new ArrayList<>();

        @JsonProperty("temperature_2m")
        private List<Double> temperature2m = new ArrayList<>();

        @JsonProperty("relative_humidity_2m")
        private List<Double> relativeHumidity2m = new ArrayList<>();

        @JsonProperty("snow_depth")
        private List<Double> snowDepth = new ArrayList<>();

        private List<Double> snowfall  = new ArrayList<>();
        private List<Double> precipitation  = new ArrayList<>();
        private List<Double> showers = new ArrayList<>();
        private List<Double> rain = new ArrayList<>();
        private List<Double> visibility = new ArrayList<>();

        @JsonProperty("wind_speed_10m")
        private List<Double> windSpeed10m = new ArrayList<>();

        @JsonProperty("cloud_cover")
        private List<Integer> cloudCover = new ArrayList<>();

        @JsonProperty("wind_speed_80m")
        private List<Double> windSpeed80m = new ArrayList<>();
    }
}

