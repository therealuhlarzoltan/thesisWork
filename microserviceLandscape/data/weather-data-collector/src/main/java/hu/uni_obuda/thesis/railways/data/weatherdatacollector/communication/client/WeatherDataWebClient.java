package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;
import reactor.core.publisher.Mono;

public interface WeatherDataWebClient {
    Mono<WeatherResponse> getWeatherByCoordinates(double latitude, double longitude);
}
