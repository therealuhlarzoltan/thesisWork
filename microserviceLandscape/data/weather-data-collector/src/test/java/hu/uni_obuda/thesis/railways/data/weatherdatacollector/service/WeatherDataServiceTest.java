package hu.uni_obuda.thesis.railways.data.weatherdatacollector.service;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway.WeatherDataGateway;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherDataServiceTest {

    @Mock
    private WeatherDataGateway weatherGateway;

    @InjectMocks
    private WeatherDataServiceImpl testedObject;

    @Test
    void getWeatherInfoByAddress_whenWeatherResponsePresent_weatherInfoBuilt() {
        String address = "Budapest-Nyugati";
        Double requestLat = 47.5;
        Double requestLon = 19.1;
        LocalDateTime dateTime = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherResponse.Hourly hourly = new WeatherResponse.Hourly();
        String timeString = "2024-10-10T10:00";
        hourly.getTime().add(timeString);
        hourly.getTemperature2m().add(5.0);
        hourly.getRelativeHumidity2m().add(80.0);
        hourly.getSnowDepth().add(0.0);
        hourly.getSnowfall().add(0.0);
        hourly.getPrecipitation().add(0.0);
        hourly.getShowers().add(0.0);
        hourly.getRain().add(0.0);
        hourly.getVisibility().add(10000);
        hourly.getWindSpeed10m().add(10.0);
        hourly.getCloudCover().add(50);
        hourly.getWindSpeed80m().add(15.0);

        double responseLat = 47.0;
        double responseLon = 19.0;

        WeatherResponse response = WeatherResponse.builder()
                .isPresent(true)
                .latitude(responseLat)
                .longitude(responseLon)
                .hourly(hourly)
                .build();

        when(weatherGateway.getWeatherByCoordinates(requestLat, requestLon, dateTime.toLocalDate()))
                .thenReturn(Mono.just(response));

        Mono<WeatherInfo> result =
                testedObject.getWeatherInfoByAddress(address, requestLat, requestLon, dateTime);

        StepVerifier.create(result)
                .assertNext(info -> {
                    assertThat(info).isNotNull();
                    assertThat(info.getAddress()).isEqualTo(address);
                    assertThat(info.getLatitude()).isEqualTo(responseLat);
                    assertThat(info.getLongitude()).isEqualTo(responseLon);
                    assertThat(info.getTime()).isEqualTo(dateTime);
                })
                .verifyComplete();

        verify(weatherGateway, times(1))
                .getWeatherByCoordinates(requestLat, requestLon, dateTime.toLocalDate());
        verifyNoMoreInteractions(weatherGateway);
    }

    @Test
    void getWeatherInfoByAddress_whenWeatherResponseNotPresent_usesRequestCoordinates() {
        String address = "Some station";
        Double requestLat = 47.5;
        Double requestLon = 19.1;
        LocalDateTime dateTime = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherResponse notPresent = WeatherResponse.builder()
                .isPresent(false)
                .build();

        when(weatherGateway.getWeatherByCoordinates(requestLat, requestLon, dateTime.toLocalDate()))
                .thenReturn(Mono.just(notPresent));

        Mono<WeatherInfo> result =
                testedObject.getWeatherInfoByAddress(address, requestLat, requestLon, dateTime);

        StepVerifier.create(result)
                .assertNext(info -> {
                    assertThat(info).isNotNull();
                    assertThat(info.getAddress()).isEqualTo(address);
                    assertThat(info.getLatitude()).isEqualTo(requestLat);
                    assertThat(info.getLongitude()).isEqualTo(requestLon);
                    assertThat(info.getTime()).isEqualTo(dateTime);
                })
                .verifyComplete();

        verify(weatherGateway, times(1))
                .getWeatherByCoordinates(requestLat, requestLon, dateTime.toLocalDate());
        verifyNoMoreInteractions(weatherGateway);
    }

    @Test
    void getWeatherInfoByAddress_whenGatewayErrors_fallbackBuilt() {
        String address = "Error station";
        Double requestLat = 47.5;
        Double requestLon = 19.1;
        LocalDateTime dateTime = LocalDateTime.of(2024, 10, 10, 10, 0);

        when(weatherGateway.getWeatherByCoordinates(requestLat, requestLon, dateTime.toLocalDate()))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        Mono<WeatherInfo> result =
                testedObject.getWeatherInfoByAddress(address, requestLat, requestLon, dateTime);

        StepVerifier.create(result)
                .assertNext(info -> {
                    assertThat(info).isNotNull();
                    assertThat(info.getAddress()).isEqualTo(address);
                    assertThat(info.getLatitude()).isEqualTo(requestLat);
                    assertThat(info.getLongitude()).isEqualTo(requestLon);
                    assertThat(info.getTime()).isEqualTo(dateTime);
                })
                .verifyComplete();

        verify(weatherGateway, times(1))
                .getWeatherByCoordinates(requestLat, requestLon, dateTime.toLocalDate());
        verifyNoMoreInteractions(weatherGateway);
    }
}
