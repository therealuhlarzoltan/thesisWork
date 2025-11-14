package hu.uni_obuda.thesis.railways.data.geocodingservice.communication.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.response.CoordinatesResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
public class MapsWebClientImpl implements MapsWebClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${maps.api.base-url}")
    private String mapsBaseUrl;
    @Value("${maps.api.geocoding-url}")
    private String geocodingUri;
    @Value("${maps.api.country-code}")
    private String countryCode;
    @Value("${maps.api.api-key}")
    private String apiKey;

    @Override
    public Mono<CoordinatesResponse> getCoordinates(String address) {
        address = address.replaceAll(",", " ") + " vasútállomás " + countryCode;
        URI requestUri = URI.create(mapsBaseUrl + geocodingUri + "?address=" + URLEncoder.encode(address, StandardCharsets.UTF_8) + "&key=" + apiKey);
        return webClient.get().uri(requestUri).exchangeToMono(apiResponse -> {
            if (apiResponse.statusCode().is2xxSuccessful()) {
                return Mono.from(apiResponse.bodyToMono(String.class))
                        .flatMap(response -> {
                            try {
                                CoordinatesResponse parsedResponse = objectMapper.readValue(response, CoordinatesResponse.class);
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
        return getUrlFromString(mapsBaseUrl + uri);
    }

    private URL getUrlFromString(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException _) {
            return null;
        }
    }
}
