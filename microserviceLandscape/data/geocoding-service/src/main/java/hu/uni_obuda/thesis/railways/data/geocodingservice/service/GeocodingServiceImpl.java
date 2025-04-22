package hu.uni_obuda.thesis.railways.data.geocodingservice.service;

import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.gateway.MapsGateway;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Service
public class GeocodingServiceImpl implements GeocodingService {

    private static final Logger LOG = LoggerFactory.getLogger(GeocodingServiceImpl.class);

    private final MapsGateway mapsGateway;

    @Override
    public Mono<GeocodingResponse> getCoordinates(String address) {
        return mapsGateway.getCoordinates(address)
            .flatMap(coordinatesResponse -> {
                if (!coordinatesResponse.isPresent()) {
                    LOG.warn("No coordinates found for address {}", address);
                }
                GeocodingResponse dto = new GeocodingResponse(coordinatesResponse.getLatitude(), coordinatesResponse.getLongitude(), address);
                return Mono.just(dto);
            })
            .onErrorResume(throwable -> {
                LOG.error("Error while getting coordinates for address {}", address, throwable);
                return Mono.just(new GeocodingResponse(null, null, address));
            });
    }
}
