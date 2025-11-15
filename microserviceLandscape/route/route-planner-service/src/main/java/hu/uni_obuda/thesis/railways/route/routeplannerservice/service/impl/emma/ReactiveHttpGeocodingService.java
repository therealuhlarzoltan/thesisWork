package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.emma;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.GeocodingGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.GeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Profile("data-source-emma")
@Primary
@Service
@Slf4j
public class ReactiveHttpGeocodingService implements GeocodingService {

    private final GeocodingGateway geocodingGateway;

    public ReactiveHttpGeocodingService(@Qualifier("reactiveGeocodingGateway") GeocodingGateway geocodingGateway) {
        this.geocodingGateway = geocodingGateway;
    }

    @Override
    public Mono<GeocodingResponse> getCoordinates(String stationName) {
        return geocodingGateway.getCoordinates(stationName);
    }
}
