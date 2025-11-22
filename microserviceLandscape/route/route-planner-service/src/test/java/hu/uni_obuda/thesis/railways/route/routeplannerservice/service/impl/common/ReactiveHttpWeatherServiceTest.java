package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.common;

import com.github.benmanes.caffeine.cache.Cache;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.WeatherDataGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveHttpWeatherServiceTest {

    @Mock
    private WeatherDataGateway weatherGateway;

    @Mock
    private Cache<String, WeatherInfo> weatherCache;

    @InjectMocks
    private ReactiveHttpWeatherService weatherService;

    @Test
    void getWeather_returnsCachedValue_whenPresent_andDoesNotCallGatewayOrPut() {
        String station = "BUD";
        Double lon = 19.0;
        Double lat = 47.5;
        LocalDateTime dateTime = LocalDateTime.now();

        String key = station + ":" + lon + ":" + lat;

        WeatherInfo cachedInfo = mock(WeatherInfo.class);
        when(weatherCache.getIfPresent(key)).thenReturn(cachedInfo);

        StepVerifier.create(weatherService.getWeather(station, lon, lat, dateTime))
                .expectNext(cachedInfo)
                .verifyComplete();

        verify(weatherCache).getIfPresent(key);
        verifyNoInteractions(weatherGateway);
        verify(weatherCache, never()).put(anyString(), any());
    }

    @Test
    void getWeather_callsGatewayOnCacheMiss_andCachesWhenNonFallbackResponse() {
        String station = "GYOR";
        Double lon = 17.6;
        Double lat = 47.7;
        LocalDateTime dateTime = LocalDateTime.now();

        String key = station + ":" + lon + ":" + lat;

        when(weatherCache.getIfPresent(key)).thenReturn(null);

        WeatherInfo gatewayResponse = mock(WeatherInfo.class);

        when(gatewayResponse.getRain()).thenReturn(1.0);

        when(weatherGateway.getWeatherInfo(station, lon, lat, dateTime))
                .thenReturn(Mono.just(gatewayResponse));

        StepVerifier.create(weatherService.getWeather(station, lon, lat, dateTime))
                .expectNext(gatewayResponse)
                .verifyComplete();

        verify(weatherCache).getIfPresent(key);
        verify(weatherGateway).getWeatherInfo(station, lon, lat, dateTime);
        verify(weatherCache).put(key, gatewayResponse);
    }

    @Test
    void getWeather_doesNotCache_whenFallbackResponseFromGateway() {
        String station = "SZEGED";
        Double lon = 20.1;
        Double lat = 46.3;
        LocalDateTime dateTime = LocalDateTime.now();

        String key = station + ":" + lon + ":" + lat;

        when(weatherCache.getIfPresent(key)).thenReturn(null);

        WeatherInfo fallbackResponse = mock(WeatherInfo.class);

        when(fallbackResponse.getRain()).thenReturn(null);
        when(fallbackResponse.getPrecipitation()).thenReturn(null);

        when(weatherGateway.getWeatherInfo(station, lon, lat, dateTime))
                .thenReturn(Mono.just(fallbackResponse));

        StepVerifier.create(weatherService.getWeather(station, lon, lat, dateTime))
                .expectNext(fallbackResponse)
                .verifyComplete();

        verify(weatherCache).getIfPresent(key);
        verify(weatherGateway).getWeatherInfo(station, lon, lat, dateTime);
        verify(weatherCache, never()).put(anyString(), any());
    }

    @Test
    void getWeather_doesNotCache_whenGatewayReturnsEmpty() {
        String station = "DEBRECEN";
        Double lon = 21.6;
        Double lat = 47.5;
        LocalDateTime dateTime = LocalDateTime.now();

        String key = station + ":" + lon + ":" + lat;

        when(weatherCache.getIfPresent(key)).thenReturn(null);
        when(weatherGateway.getWeatherInfo(station, lon, lat, dateTime))
                .thenReturn(Mono.empty());

        StepVerifier.create(weatherService.getWeather(station, lon, lat, dateTime))
                .verifyComplete();

        verify(weatherCache).getIfPresent(key);
        verify(weatherGateway).getWeatherInfo(station, lon, lat, dateTime);
        verify(weatherCache, never()).put(anyString(), any());
    }

    @Test
    void getWeather_doesNotCache_whenGatewayErrors() {
        String station = "MISKOLC";
        Double lon = 20.8;
        Double lat = 48.1;
        LocalDateTime dateTime = LocalDateTime.now();

        String key = station + ":" + lon + ":" + lat;

        when(weatherCache.getIfPresent(key)).thenReturn(null);

        RuntimeException error = new RuntimeException("Gateway failure");
        when(weatherGateway.getWeatherInfo(station, lon, lat, dateTime))
                .thenReturn(Mono.error(error));

        StepVerifier.create(weatherService.getWeather(station, lon, lat, dateTime))
                .expectErrorMatches(t -> t == error)
                .verify();

        verify(weatherCache).getIfPresent(key);
        verify(weatherGateway).getWeatherInfo(station, lon, lat, dateTime);
        verify(weatherCache, never()).put(anyString(), any());
    }
}
