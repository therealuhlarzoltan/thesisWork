package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Primary
@Component
public class ReactiveStationWebClient implements StationWebClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${app.delay-data-collector-url}")
    private String baseUrl;
    @Value("${app.delay-data-collector-train-route-uri}")
    private String trainRouteUri;
    @Value("${app.delay-data-collector-train-station-uri}")
    private String trainStationUri;


    @Override
    public Mono<TrainStationResponse> makeStationRequest(String stationCode) {
        URI stationUri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(trainStationUri)
                .queryParam("stationCode", stationCode)
                .build(false)
                .toUri();

        return webClient.get()
                .uri(stationUri.toString())
                .exchangeToMono(apiResponse -> {
                    if (apiResponse.statusCode().is2xxSuccessful()) {
                        return apiResponse.bodyToMono(String.class)
                                .flatMap(response -> {
                                    try {
                                        List<TrainStationResponse> parsedList = objectMapper.readValue(
                                                response,
                                                objectMapper.getTypeFactory().constructCollectionType(List.class, TrainStationResponse.class)
                                        );

                                        if (parsedList.isEmpty()) {
                                            return Mono.error(new EntityNotFoundException());
                                        } else {
                                            return Mono.just(parsedList.getFirst());
                                        }

                                    } catch (IOException ioException) {
                                        return Mono.error(mapMappingExceptionToException(ioException, stationUri.toString()));
                                    }
                                });
                    } else {
                        return Mono.error(mapApiResponseToException(apiResponse));
                    }
                });
    }

    @Override
    public Mono<TrainRouteResponse> makeTrainRouteRequest(String trainNumber) {
        URI routeUri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(trainRouteUri)
                .queryParam("trainNumber", trainNumber)
                .build(false)
                .toUri();
        return webClient.get()
                .uri(routeUri.toString())
                .exchangeToMono(apiResponse -> {
                    if (apiResponse.statusCode().is2xxSuccessful()) {
                        return apiResponse.bodyToMono(String.class)
                                .flatMap(response -> {
                                    try {
                                        List<TrainRouteResponse> parsedList = objectMapper.readValue(
                                                response,
                                                objectMapper.getTypeFactory().constructCollectionType(List.class, TrainRouteResponse.class)
                                        );

                                        if (parsedList.isEmpty()) {
                                            return Mono.error(new EntityNotFoundException());
                                        } else {
                                            return Mono.just(parsedList.getFirst());
                                        }

                                    } catch (IOException ioException) {
                                        return Mono.error(mapMappingExceptionToException(ioException, routeUri.toString()));
                                    }
                                });
                    } else {
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
        return getUrlFromString(baseUrl + uri);
    }

    private URL getUrlFromString(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException _) {
            return null;
        }
    }
}
