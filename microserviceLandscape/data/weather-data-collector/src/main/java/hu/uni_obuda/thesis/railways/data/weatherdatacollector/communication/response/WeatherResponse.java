package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response;

import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

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
        private List<String> time;

        @JsonProperty("temperature_2m")
        private List<Double> temperature2m;

        @JsonProperty("relative_humidity_2m")
        private List<Integer> relativeHumidity2m;

        @JsonProperty("snow_depth")
        private List<Integer> snowDepth;

        private List<Integer> snowfall;
        private List<Double> precipitation;
        private List<Integer> showers;
        private List<Double> rain;
        private List<Integer> visibility;

        @JsonProperty("wind_speed_10m")
        private List<Double> windSpeed10m;

        @JsonProperty("cloud_cover")
        private List<Integer> cloudCover;

        @JsonProperty("wind_speed_80m")
        private List<Double> windSpeed80m;
    }
}

