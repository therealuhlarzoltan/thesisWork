package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Primary
@Component
public class ReactiveWeatherWebClient implements WeatherWebClient {

    private final WebClient webClient;

    @Value("${app.weather-data-collector-url}")
    private String baseUrl;
    @Value("${app.weather-data-collector-collect-weather-uri}")
    private String weatherUri;

    @Override
    public Mono<WeatherInfo> makeWeatherRequest(String address, Double latitude, Double longitude, LocalDateTime date) {
        URI weatherRequestUri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(weatherUri)
                .queryParam("stationName", address)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("dateTime", date.toString())
                .build(false)
                .toUri();

        return webClient.get()
                .uri(weatherRequestUri.toString())
                .retrieve()
                .bodyToMono(WeatherInfo.class);
    }
}
