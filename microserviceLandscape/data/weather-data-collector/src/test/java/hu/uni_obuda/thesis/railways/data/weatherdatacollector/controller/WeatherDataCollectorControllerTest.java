package hu.uni_obuda.thesis.railways.data.weatherdatacollector.controller;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.service.WeatherDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherDataCollectorControllerTest {

    @Mock
    private WeatherDataService weatherDataService;

    @InjectMocks
    private WeatherDataCollectorController controller;

    @Test
    void getWeatherInfo_successfulServiceCall_returnsWeatherInfo() {
        String stationName = "Budapest-Nyugati";
        Double latitude = 47.507;
        Double longitude = 19.045;
        LocalDateTime dateTime = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherInfo expected = mock(WeatherInfo.class);

        when(weatherDataService.getWeatherInfoByAddress(stationName, latitude, longitude, dateTime))
                .thenReturn(Mono.just(expected));

        Mono<WeatherInfo> result =
                controller.getWeatherInfo(stationName, latitude, longitude, dateTime);

        StepVerifier.create(result)
                .expectNext(expected)
                .verifyComplete();

        verify(weatherDataService, times(1))
                .getWeatherInfoByAddress(stationName, latitude, longitude, dateTime);
        verifyNoMoreInteractions(weatherDataService);
    }

    @Test
    void getWeatherInfo_failedServiceCall_returnsFallback() {
        String stationName = "Some invalid station";
        Double latitude = 0.0;
        Double longitude = 0.0;
        LocalDateTime dateTime = LocalDateTime.of(2024, 10, 10, 10, 0);

        WeatherInfo fallback = mock(WeatherInfo.class);

        when(weatherDataService.getWeatherInfoByAddress(stationName, latitude, longitude, dateTime))
                .thenReturn(Mono.just(fallback));

        Mono<WeatherInfo> result =
                controller.getWeatherInfo(stationName, latitude, longitude, dateTime);

        StepVerifier.create(result)
                .expectNext(fallback)
                .verifyComplete();

        verify(weatherDataService, times(1))
                .getWeatherInfoByAddress(stationName, latitude, longitude, dateTime);
        verifyNoMoreInteractions(weatherDataService);
    }
}
