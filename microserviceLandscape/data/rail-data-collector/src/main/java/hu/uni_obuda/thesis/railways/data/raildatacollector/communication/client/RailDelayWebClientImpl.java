package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.*;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RailDelayWebClientImpl implements RailDelayWebClient {

    private static final Logger LOG = LoggerFactory.getLogger(RailDelayWebClientImpl.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${railway.api.base-url}")
    private String railwayBaseUrl;
    @Value("${railway.api.time-table-getter-uri}")
    private String timetableGetterUri;
    @Value("${railway.api.train-details-getter-uri}")
    private String trainDetailsGetterUri;

    @Override
    public Mono<ShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date) {
        URI timetableUri = UriComponentsBuilder.fromPath(timetableGetterUri)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("date", date.toString())
                .build().toUri();
        return webClient.get().uri(timetableUri.toString())
        .exchangeToMono(apiResponse -> {
            if (apiResponse.statusCode().is2xxSuccessful()) {
                return Mono.from(apiResponse.bodyToMono(String.class))
                        .flatMap(response -> {
                            try {
                                ShortTimetableResponse parsedResponse = objectMapper.readValue(response, ShortTimetableResponse.class);
                                parsedResponse.removeUnnecessaryData();
                                return Mono.just(parsedResponse);
                            } catch (IOException ioException) {
                                return Mono.error(mapMappingExceptionToException(ioException, timetableUri.toString()));
                            }
                        });
            } else {
                return Mono.error(mapApiResponseToException(apiResponse));
            }
        });
    }

    @Override
    public Mono<ShortTrainDetailsResponse> getShortTrainDetails(String thirdPartyUrl) {
        URI trainDetailsUri = URI.create(railwayBaseUrl + trainDetailsGetterUri + "?url=" + URLEncoder.encode(thirdPartyUrl, StandardCharsets.UTF_8));
        return webClient.get().uri(trainDetailsUri).exchangeToMono(apiResponse -> {
            if (apiResponse.statusCode().is2xxSuccessful()) {
                return Mono.from(apiResponse.bodyToMono(String.class))
                        .flatMap(response -> {
                            try {
                                ShortTrainDetailsResponse parsedResponse = objectMapper.readValue(response, ShortTrainDetailsResponse.class);
                                return Mono.just(parsedResponse);
                            } catch (IOException ioException) {
                                return Mono.error(mapMappingExceptionToException(ioException, trainDetailsUri.toString()));
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
