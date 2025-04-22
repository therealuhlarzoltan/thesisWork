package hu.uni_obuda.thesis.railways.data.geocodingservice.service;

import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.gateway.MapsGateway;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Service
public class GeocodingServiceImpl implements GeocodingService {

    private final MapsGateway mapsGateway;

    @Override
    public Mono<GeocodingResponse> getCoordinates(String address) {
        return mapsGateway.getCoordinates(address)
            .flatMap(coordinatesResponse -> {
                if (coordinatesResponse.isPresent()) {
                    GeocodingResponse dto = new GeocodingResponse(coordinatesResponse.getLatitude(), coordinatesResponse.getLongitude());
                    return Mono.just(dto);
                } else {
                    return Mono.error(new InternalApiException("Could not retrieve coordinates", null));
                }
        });
    }
}
