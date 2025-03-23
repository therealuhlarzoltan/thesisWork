package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.*;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class RailDelayWebClientImpl implements RailDelayWebClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${railway.api.base-url}")
    private String railwayBaseUrl;
    @Value("${railway.api.time-table-getter-uri}")
    private String timetableGetterUri;
    @Value("${railway.api.train-details-getter-uri}")
    private String trainDetailsGetterUri;

    @Override
    public Mono<ShortTimetableResponse> getShortTimetable(String from, String to) {
        URI timetableUri = UriComponentsBuilder.fromUriString(timetableGetterUri)
                .build(from, to);
        return webClient.get().uri(timetableUri).exchangeToMono(apiResponse -> {
            if (apiResponse.statusCode().is2xxSuccessful()) {
                return Mono.from(apiResponse.bodyToMono(String.class))
                        .flatMap(response -> {
                            try {
                                ShortTimetableResponse parsedResponse = objectMapper.readValue(response, ShortTimetableResponse.class);
                                return Mono.just(parsedResponse);
                            } catch (IOException ioException) {
                                return Mono.error(mapMappingExceptionToException(ioException, timetableGetterUri));
                            }
                        });
            } else {
                return Mono.error(mapApiResponseToException(apiResponse));
            }
        });
    }

    @Override
    public Mono<ShortTrainDetailsResponse> getShortTrainDetails(String thirdPartyUrl) {
        URI trainDetailsUri = UriComponentsBuilder.fromUriString(thirdPartyUrl)
                .build(thirdPartyUrl);
        return webClient.get().uri(trainDetailsGetterUri).exchangeToMono(apiResponse -> {
            if (apiResponse.statusCode().is2xxSuccessful()) {
                return Mono.from(apiResponse.bodyToMono(String.class))
                        .flatMap(response -> {
                            try {
                                ShortTrainDetailsResponse parsedResponse = objectMapper.readValue(response, ShortTrainDetailsResponse.class);
                                return Mono.just(parsedResponse);
                            } catch (IOException ioException) {
                                return Mono.error(mapMappingExceptionToException(ioException, timetableGetterUri));
                            }
                        });
            } else {
                return Mono.error(mapApiResponseToException(apiResponse));
            }
        });
    }

    private RuntimeException mapApiResponseToException(ClientResponse clientResponse) {
        return new ExternalApiException(clientResponse.statusCode(), getUrlFromString(railwayBaseUrl + clientResponse.request().getURI().toString()));
    }

    private RuntimeException mapMappingExceptionToException(IOException ioException, String uri) {
        return new ExternalApiFormatMismatchException(ioException.getMessage(), ioException, getUrlFromUriString(uri));
    }

    private URL getUrlFromUriString(String uri) {
        return getUrlFromString(railwayBaseUrl + uri);
    }

    private URL getUrlFromString(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException _) {
            return null;
        }
    }
}
