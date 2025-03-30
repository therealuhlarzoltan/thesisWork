package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Component
public class WeatherDataWebClientImpl implements WeatherDataWebClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String HOURLY_PARAMETERS = "temperature_2m,relative_humidity_2m,snow_depth,snowfall,precipitation,showers,rain,visibility,wind_speed_10m,cloud_cover,wind_speed_80m";

    @Value("${weather.api.base-url}")
    private String weatherBaseUrl;
    @Value("${weather.api.forecast-url}")
    private String forecastUri;
    @Value("${weather.api.time-zone}")
    private String timeZone;

    @Autowired
    public WeatherDataWebClientImpl(@Qualifier("weatherWebClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public Mono<WeatherResponse> getWeatherByCoordinates(double latitude, double longitude, LocalDate date) {
        URI requestUri = UriComponentsBuilder.fromPath(forecastUri)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("timezone", timeZone)
                .queryParam("start_date", date.toString())
                .queryParam("end_date", date.toString())
                .queryParam("hourly", HOURLY_PARAMETERS)
                .build(false).encode(StandardCharsets.UTF_8).toUri();

        return webClient.get().uri(requestUri.toString()).exchangeToMono(apiResponse -> {
            if (apiResponse.statusCode().is2xxSuccessful()) {
                return Mono.from(apiResponse.bodyToMono(String.class))
                        .flatMap(response -> {
                            try {
                                WeatherResponse parsedResponse = objectMapper.readValue(response, WeatherResponse.class);
                                return Mono.just(parsedResponse);
                            } catch (IOException ioException) {
                                return Mono.error(mapMappingExceptionToException(ioException, requestUri.toString()));
                            }
                        });
            }  else {
                return Mono.error(mapApiResponseToException(apiResponse));
            }
        });
    }

    private RuntimeException mapApiResponseToException(ClientResponse clientResponse) {
        return new ExternalApiException(clientResponse.statusCode(), getUrlFromString(clientResponse.request().getURI().toString()));
    }

    private RuntimeException mapMappingExceptionToException(IOException ioException, String uri) {
        return new ExternalApiFormatMismatchException(ioException.getMessage(), ioException, getUrlFromUriString(uri));
    }

    private URL getUrlFromUriString(String uri) {
        return getUrlFromString(weatherBaseUrl + uri);
    }

    private URL getUrlFromString(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException _) {
            return null;
        }
    }

}
