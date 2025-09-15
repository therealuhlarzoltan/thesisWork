package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.*;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource.CachingYamlGraphQlVariableLoader;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Profile("data-source-emma")
@Component
@RequiredArgsConstructor
public class GraphQlWebClientImpl implements RailDelayWebClient {

    private final HttpGraphQlClient graphQlClient;
    private final CachingYamlGraphQlVariableLoader variableLoader;

    @Value("${railway.api.graphql.short-timetable-document}")
    private String shortTimeTableDocumentName;
    @Value("${railway.api.graphql.long-timetable-document}")
    private String longTimeTableDocumentName;
    @Value("${railway.api.graphql.short-train-details-document}")
    private String shortTrainDetailsDocumentName;
    @Value("${railway.api.base-url}")
    private String railwayBaseUrl;
    @Value("${railway.api.time-table-getter-uri}")
    private String timetableGetterUri;
    @Value("${railway.api.train-details-getter-uri}")
    private String trainDetailsGetterUri;

    @Override
    public Mono<GraphQlShortTimetableResponse> getShortTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        var variableMap =  Map.<String, Object>of("fromPlace", concatenatePlaceWithCoordinates(from, fromLatitude, fromLongitude), "toPlace", concatenatePlaceWithCoordinates(to, toLatitude, toLongitude), "date", date.toString());
        return graphQlClient.documentName(shortTimeTableDocumentName)
                .variables(mergeWithDefaultVariables(variableMap, shortTimeTableDocumentName)
                )
                .execute()
                .flatMap(clientGraphQlResponse -> {
                    if (clientGraphQlResponse.isValid()) {
                        try {
                            GraphQlShortTimetableResponse parsedResponse = clientGraphQlResponse.field("data").toEntity(GraphQlShortTimetableResponse.class);
                            parsedResponse.removeUnnecessaryData();
                            return Mono.just(parsedResponse);
                        } catch (RuntimeException e) {
                            return Mono.error(new ExternalApiFormatMismatchException("Could not parse short timetable response", e, getUrlFromUriString(timetableGetterUri)));
                        }
                    } else {
                        return Mono.error(new ExternalApiException(HttpStatusCode.valueOf(400), getUrlFromUriString(timetableGetterUri), clientGraphQlResponse.getErrors().isEmpty() ? "" : clientGraphQlResponse.getErrors().getFirst().getMessage()));
                    }
                });
    }

    @Override
    public Mono<GraphQlShortTrainDetailsResponse> getShortTrainDetails(String trainId, LocalDate serviceDate) {
        var variableMap =  Map.<String, Object>of("gtfsId", trainId, "serviceDay", serviceDate.toString());
        return graphQlClient.documentName(shortTrainDetailsDocumentName)
                .variables(mergeWithDefaultVariables(variableMap, shortTrainDetailsDocumentName)
                )
                .execute()
                .flatMap(clientGraphQlResponse -> {
                    if (clientGraphQlResponse.isValid()) {
                        try {
                            GraphQlShortTrainDetailsResponse parsedResponse = clientGraphQlResponse.field("data").toEntity(GraphQlShortTrainDetailsResponse.class);
                            return Mono.just(parsedResponse);
                        } catch (RuntimeException e) {
                            return Mono.error(new ExternalApiFormatMismatchException("Could not parse short train details response", e, getUrlFromUriString(timetableGetterUri)));
                        }
                    } else {
                        return Mono.error(new ExternalApiException(HttpStatusCode.valueOf(400), getUrlFromUriString(trainDetailsGetterUri), clientGraphQlResponse.getErrors().isEmpty() ? "" : clientGraphQlResponse.getErrors().getFirst().getMessage()));
                    }
                });
    }

    @Override
    public Mono<GraphQlTimetableResponse> getTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return Mono.error(new UnsupportedOperationException("Not implemented yet"));
    }

    private Map<String, Object> mergeWithDefaultVariables(Map<String, Object> dynamicVariables, String documentName) {
        Map<String, Object> mergedVariables = new HashMap<>(variableLoader.loadForDocument(documentName).asMap());
        mergedVariables.putAll(dynamicVariables);
        return mergedVariables;
    }

    private String concatenatePlaceWithCoordinates(String place, double latitude, double longitude) {
        return place + "::" + latitude + "," + longitude;
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
