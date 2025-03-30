package hu.uni_obuda.thesis.railways.data.weatherdatacollector.util;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;

import java.time.LocalDateTime;
import java.util.Objects;

public class WeatherResponseUtils {

    public static Integer extractCloudCover(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if  (index == -1) {
            return null;
        } else if (weatherResponse.getHourly().getCloudCover().size() < index + 1) {
            return null;
        } else {
            return weatherResponse.getHourly().getCloudCover().get(index);
        }
    }

    public static Double extractTemperature(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if  (index == -1) {
            return null;
        } else if (weatherResponse.getHourly().getTemperature2m().size() < index + 1) {
            return null;
        } else {
            return weatherResponse.getHourly().getTemperature2m().get(index);
        }
    }

    public static Double extractRelativeHumidity(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if  (index == -1) {
            return null;
        } else if (weatherResponse.getHourly().getRelativeHumidity2m().size() < index + 1) {
            return null;
        } else {
            return weatherResponse.getHourly().getRelativeHumidity2m().get(index);
        }
    }

    public static Double extractSnowDepth(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if  (index == -1) {
            return null;
        } else if (weatherResponse.getHourly().getSnowDepth().size() < index + 1) {
            return null;
        } else {
            return weatherResponse.getHourly().getSnowDepth().get(index);
        }
    }

    public static Boolean extractIsSnowing(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        Double snowFall = extractSnowFall(weatherResponse, dateTime);
        if (snowFall == null) {
            return null;
        } else {
            return snowFall > 0;
        }
    }

    public static Double extractRain(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if (index == -1 || index >= weatherResponse.getHourly().getRain().size()) {
            return null;
        } else if (index == weatherResponse.getHourly().getRain().size() - 1) {
            return weatherResponse.getHourly().getRain().get(index);
        } else {
            return weatherResponse.getHourly().getRain().get(index + 1);
        }
    }

    public static Double extractShowers(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if (index == -1 || index >= weatherResponse.getHourly().getShowers().size()) {
            return null;
        } else if (index == weatherResponse.getHourly().getShowers().size() - 1) {
            return weatherResponse.getHourly().getShowers().get(index);
        } else {
            return weatherResponse.getHourly().getShowers().get(index + 1);
        }
    }

    public static Double extreactPrecipitation(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if (index == -1 || index >= weatherResponse.getHourly().getPrecipitation().size()) {
            return null;
        } else if (index == weatherResponse.getHourly().getPrecipitation().size() - 1) {
            return weatherResponse.getHourly().getPrecipitation().get(index);
        } else {
            return weatherResponse.getHourly().getPrecipitation().get(index + 1);
        }
    }

    public static Double extractSnowFall(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if (index == -1 || index >= weatherResponse.getHourly().getSnowfall().size()) {
            return null;
        } else if (index == weatherResponse.getHourly().getSnowfall().size() - 1) {
            return weatherResponse.getHourly().getSnowfall().get(index);
        } else {
            return weatherResponse.getHourly().getSnowfall().get(index + 1);
        }
    }

    public static Boolean extractIsRaining(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        Double showers = extractSnowFall(weatherResponse, dateTime);
        Double rain = extractRain(weatherResponse, dateTime);
        if (showers == null && rain == null) {
            return null;
        } else if (showers != null && rain != null) {
            return showers > 0 || rain > 0;
        } else {
            return Objects.requireNonNullElse(showers, rain) > 0;
        }
    }

    public static Double extractWindSpeedAt10m(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if  (index == -1) {
            return null;
        } else if (weatherResponse.getHourly().getWindSpeed10m().size() < index + 1) {
            return null;
        } else {
            return weatherResponse.getHourly().getWindSpeed10m().get(index);
        }
    }

    public static Double extractWindSpeedAt80m(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if  (index == -1) {
            return null;
        } else if (weatherResponse.getHourly().getWindSpeed80m().size() < index + 1) {
            return null;
        } else {
            return weatherResponse.getHourly().getWindSpeed80m().get(index);
        }
    }

    public static Double extractVisibility(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        int index = getHourIndex(weatherResponse, dateTime);
        if (index == -1) {
            return null;
        } else if (weatherResponse.getHourly().getVisibility().size() < index + 1) {
            return null;
        } else {
            return weatherResponse.getHourly().getVisibility().get(index);
        }
    }

    private static int getHourIndex(WeatherResponse weatherResponse, LocalDateTime dateTime) {
        String hour = String.format("%02d", dateTime.getHour());
        return weatherResponse.getHourly().getTime().stream().map(t -> t.split("T")[1].substring(0, 2)).toList().indexOf(hour);
    }

}
