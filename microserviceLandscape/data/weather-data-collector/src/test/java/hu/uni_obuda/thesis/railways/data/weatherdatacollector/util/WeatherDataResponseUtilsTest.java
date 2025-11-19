package hu.uni_obuda.thesis.railways.data.weatherdatacollector.util;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static hu.uni_obuda.thesis.railways.data.weatherdatacollector.util.WeatherResponseUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

class WeatherResponseUtilsTest {

    private WeatherResponse wrap(WeatherResponse.Hourly hourly) {
        WeatherResponse response = new WeatherResponse();
        response.setHourly(hourly);
        return response;
    }

    @Test
    void extractors_returnNull_whenHourNotFound() {
        LocalDateTime dt = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherResponse.Hourly hourly = new WeatherResponse.Hourly();
        hourly.getTime().add("2024-10-10T09:00");

        hourly.getCloudCover().add(10);
        hourly.getTemperature2m().add(1.0);
        hourly.getRelativeHumidity2m().add(60.0);
        hourly.getSnowDepth().add(0.0);
        hourly.getRain().add(0.0);
        hourly.getShowers().add(0.0);
        hourly.getPrecipitation().add(0.0);
        hourly.getSnowfall().add(0.0);
        hourly.getWindSpeed10m().add(5.0);
        hourly.getWindSpeed80m().add(15.0);
        hourly.getVisibility().add(1000);

        WeatherResponse response = wrap(hourly);

        assertThat(extractCloudCover(response, dt)).isNull();
        assertThat(extractTemperature(response, dt)).isNull();
        assertThat(extractRelativeHumidity(response, dt)).isNull();
        assertThat(extractSnowDepth(response, dt)).isNull();
        assertThat(extractRain(response, dt)).isNull();
        assertThat(extractShowers(response, dt)).isNull();
        assertThat(extreactPrecipitation(response, dt)).isNull();
        assertThat(extractSnowFall(response, dt)).isNull();
        assertThat(extractIsSnowing(response, dt)).isNull();
        assertThat(extractIsRaining(response, dt)).isNull();
        assertThat(extractWindSpeedAt10m(response, dt)).isNull();
        assertThat(extractWindSpeedAt80m(response, dt)).isNull();
        assertThat(extractVisibility(response, dt)).isNull();
    }

    @Test
    void extractors_returnNull_whenIndexOutOfRangeForLists() {
        LocalDateTime dt = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherResponse.Hourly hourly = new WeatherResponse.Hourly();
        hourly.getTime().add("2024-10-10T09:00");
        hourly.getTime().add("2024-10-10T10:00");

        hourly.getCloudCover().add(10);
        hourly.getTemperature2m().add(1.0);
        hourly.getRelativeHumidity2m().add(60.0);
        hourly.getSnowDepth().add(0.0);
        hourly.getRain().add(0.5);
        hourly.getShowers().add(0.5);
        hourly.getPrecipitation().add(0.5);
        hourly.getSnowfall().add(0.5);
        hourly.getWindSpeed10m().add(5.0);
        hourly.getWindSpeed80m().add(15.0);
        hourly.getVisibility().add(1000);

        WeatherResponse response = wrap(hourly);

        assertThat(extractCloudCover(response, dt)).isNull();
        assertThat(extractTemperature(response, dt)).isNull();
        assertThat(extractRelativeHumidity(response, dt)).isNull();
        assertThat(extractSnowDepth(response, dt)).isNull();
        assertThat(extractWindSpeedAt10m(response, dt)).isNull();
        assertThat(extractWindSpeedAt80m(response, dt)).isNull();
        assertThat(extractVisibility(response, dt)).isNull();

        assertThat(extractRain(response, dt)).isNull();
        assertThat(extractShowers(response, dt)).isNull();
        assertThat(extreactPrecipitation(response, dt)).isNull();
        assertThat(extractSnowFall(response, dt)).isNull();

        assertThat(extractIsSnowing(response, dt)).isNull();
        assertThat(extractIsRaining(response, dt)).isNull();
    }

    @Test
    void extractors_middleIndex_useCurrentOrNextElementAndIsSnowingRainingTrue() {
        LocalDateTime dt = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherResponse.Hourly hourly = new WeatherResponse.Hourly();
        hourly.getTime().add("2024-10-10T09:00");
        hourly.getTime().add("2024-10-10T10:00");
        hourly.getTime().add("2024-10-10T11:00");

        hourly.getCloudCover().add(10);
        hourly.getCloudCover().add(20);
        hourly.getCloudCover().add(30);

        hourly.getTemperature2m().add(1.0);
        hourly.getTemperature2m().add(2.0);
        hourly.getTemperature2m().add(3.0);

        hourly.getRelativeHumidity2m().add(60.0);
        hourly.getRelativeHumidity2m().add(70.0);
        hourly.getRelativeHumidity2m().add(80.0);

        hourly.getSnowDepth().add(0.0);
        hourly.getSnowDepth().add(1.0);
        hourly.getSnowDepth().add(2.0);

        hourly.getWindSpeed10m().add(5.0);
        hourly.getWindSpeed10m().add(6.0);
        hourly.getWindSpeed10m().add(7.0);

        hourly.getWindSpeed80m().add(15.0);
        hourly.getWindSpeed80m().add(16.0);
        hourly.getWindSpeed80m().add(17.0);

        hourly.getVisibility().add(1000);
        hourly.getVisibility().add(2000);
        hourly.getVisibility().add(3000);

        hourly.getRain().add(0.0);
        hourly.getRain().add(1.0);
        hourly.getRain().add(2.0);

        hourly.getShowers().add(0.0);
        hourly.getShowers().add(2.0);
        hourly.getShowers().add(3.0);

        hourly.getPrecipitation().add(0.1);
        hourly.getPrecipitation().add(0.2);
        hourly.getPrecipitation().add(0.3);

        hourly.getSnowfall().add(0.0);
        hourly.getSnowfall().add(0.0);
        hourly.getSnowfall().add(1.0);

        WeatherResponse response = wrap(hourly);

        assertThat(extractCloudCover(response, dt)).isEqualTo(20);
        assertThat(extractTemperature(response, dt)).isEqualTo(2.0);
        assertThat(extractRelativeHumidity(response, dt)).isEqualTo(70.0);
        assertThat(extractSnowDepth(response, dt)).isEqualTo(1.0);
        assertThat(extractWindSpeedAt10m(response, dt)).isEqualTo(6.0);
        assertThat(extractWindSpeedAt80m(response, dt)).isEqualTo(16.0);
        assertThat(extractVisibility(response, dt)).isEqualTo(2000);

        assertThat(extractRain(response, dt)).isEqualTo(2.0);
        assertThat(extractShowers(response, dt)).isEqualTo(3.0);
        assertThat(extreactPrecipitation(response, dt)).isEqualTo(0.3);
        assertThat(extractSnowFall(response, dt)).isEqualTo(1.0);

        assertThat(extractIsSnowing(response, dt)).isTrue();

        assertThat(extractIsRaining(response, dt)).isTrue();
    }

    @Test
    void rainLikeExtractors_lastIndex_useSameIndexElement() {
        LocalDateTime dt = LocalDateTime.of(2024, 10, 10, 11, 0);

        WeatherResponse.Hourly hourly = new WeatherResponse.Hourly();
        hourly.getTime().add("2024-10-10T09:00");
        hourly.getTime().add("2024-10-10T10:00");
        hourly.getTime().add("2024-10-10T11:00");

        hourly.getRain().add(0.0);
        hourly.getRain().add(1.0);
        hourly.getRain().add(5.0);

        hourly.getShowers().add(0.0);
        hourly.getShowers().add(2.0);
        hourly.getShowers().add(6.0);

        hourly.getPrecipitation().add(0.1);
        hourly.getPrecipitation().add(0.2);
        hourly.getPrecipitation().add(0.9);

        hourly.getSnowfall().add(0.0);
        hourly.getSnowfall().add(0.0);
        hourly.getSnowfall().add(3.0);

        WeatherResponse response = wrap(hourly);

        assertThat(extractRain(response, dt)).isEqualTo(5.0);
        assertThat(extractShowers(response, dt)).isEqualTo(6.0);
        assertThat(extreactPrecipitation(response, dt)).isEqualTo(0.9);
        assertThat(extractSnowFall(response, dt)).isEqualTo(3.0);

        assertThat(extractIsSnowing(response, dt)).isTrue();
    }

    @Test
    void isRaining_whenOnlyOneSourceNonNull_usesObjectsRequireNonNullElseBranch() {
        LocalDateTime dt = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherResponse.Hourly hourly = new WeatherResponse.Hourly();
        hourly.getTime().add("2024-10-10T10:00");

        hourly.getRain().add(2.0);

        WeatherResponse response = wrap(hourly);

        assertThat(extractIsRaining(response, dt)).isTrue();
    }

    @Test
    void isSnowing_returnsNullWhenSnowFallNull() {
        LocalDateTime dt = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherResponse.Hourly hourly = new WeatherResponse.Hourly();
        hourly.getTime().add("2024-10-10T10:00");

        WeatherResponse response = wrap(hourly);

        assertThat(extractIsSnowing(response, dt)).isNull();
    }

    @Test
    void isRaining_returnsNullWhenBothShowersAndRainNull() {
        LocalDateTime dt = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherResponse.Hourly hourly = new WeatherResponse.Hourly();
        hourly.getTime().add("2024-10-10T10:00");

        WeatherResponse response = wrap(hourly);

        assertThat(extractIsRaining(response, dt)).isNull();
    }
}
