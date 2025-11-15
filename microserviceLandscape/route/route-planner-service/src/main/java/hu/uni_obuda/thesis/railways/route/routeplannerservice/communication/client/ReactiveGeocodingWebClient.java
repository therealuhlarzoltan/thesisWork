package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@Profile("data-source-emma")
@Primary
@Component
@RequiredArgsConstructor
public class ReactiveGeocodingWebClient implements GeocodingWebClient {

    private final WebClient webClient;

    @Value("${app.geocoding-service-url}")
    private String baseUrl;
    @Value("${app.geocoding-service-geocoding-uri}")
    private String geocodingUri;

    @Override
    public Mono<GeocodingResponse> makeGeocodingRequest(String stationName) {
        URI geocoderUri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(geocodingUri)
                .queryParam("address", stationName)
                .build(false)
                .toUri();
        return webClient.get().uri(geocoderUri.toString()).retrieve().bodyToMono(GeocodingResponse.class);
    }
}
