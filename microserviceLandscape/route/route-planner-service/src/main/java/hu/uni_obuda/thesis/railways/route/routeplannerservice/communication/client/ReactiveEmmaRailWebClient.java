package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;

@Profile("data-source-emma")
@Primary
@Component
@Slf4j
@RequiredArgsConstructor
public class ReactiveEmmaRailWebClient implements EmmaRailWebClient {

    private final WebClient webClient;

    @Value("${app.rail-data-collector-url}")
    private String baseUrl;
    @Value("${app.rail-data-collector-route-planner-uri}")
    private String routePlannerUri;

    @Override
    public Flux<TrainRouteResponse> makeRouteRequest(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        URI routeUri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(routePlannerUri)
                .queryParam("from", from)
                .queryParam("fromLatitude", fromLatitude)
                .queryParam("fromLongitude", fromLongitude)
                .queryParam("to", to)
                .queryParam("toLatitude", toLatitude)
                .queryParam("toLongitude", toLongitude)
                .queryParam("date", date.toString())
                .build(false)
                .toUri();
        return webClient.get()
                .uri(routeUri.toString())
                .exchangeToFlux(apiResponse -> {
                    if (apiResponse.statusCode().is2xxSuccessful()) {
                        return apiResponse.bodyToFlux(TrainRouteResponse.class);
                    } else {
                        return Flux.error(mapApiResponseToException(apiResponse));
                    }
                });
    }

    private RuntimeException mapApiResponseToException(ClientResponse clientResponse) {
        if (clientResponse.statusCode().equals(HttpStatusCode.valueOf(404))) {
            return new EntityNotFoundException("", Object.class);
        }
        return new ExternalApiException(clientResponse.statusCode(), getUrlFromString(clientResponse.request().getURI().toString()));
    }


    private URL getUrlFromString(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException _) {
            return null;
        }
    }
}
