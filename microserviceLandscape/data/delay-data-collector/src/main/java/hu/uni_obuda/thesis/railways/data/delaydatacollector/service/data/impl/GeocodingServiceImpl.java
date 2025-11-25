package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.impl;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.CoordinatesCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainStationRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.GeocodingService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.sender.MessageSender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.registry.CoordinatesRegistry;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingRequest;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class GeocodingServiceImpl implements GeocodingService {

    private final static Logger LOG = LoggerFactory.getLogger(GeocodingServiceImpl.class);

    private final CoordinatesCache cache;
    private final TrainStationRepository repository;
    private final CoordinatesRegistry registry;
    private final MessageSender messageSender;

    @Override
    public Mono<GeocodingResponse> getCoordinatesByStation(String stationName) {
        return cache.isCached(stationName)
                .flatMap(isCached -> {
                    if (Boolean.TRUE.equals(isCached)) {
                        LOG.info("Coordinates for station {} are already cached, reusing them", stationName);
                        return cache.get(stationName);
                    } else {
                        return repository.findById(stationName)
                                .flatMap(station -> {
                                    if (station.getLatitude() != null && station.getLongitude() != null) {
                                        LOG.info("Coordinates for station {} are found in the database, caching and returning them", stationName);
                                        GeocodingResponse coordinates = new GeocodingResponse(station.getLatitude(), station.getLongitude(), station.getStationCode());
                                        return cache.cache(stationName, coordinates).thenReturn(coordinates);
                                    } else {
                                        LOG.info("Coordinates for station {} are not found in the database, attempting to get them", stationName);
                                        return fetchCoordinates(stationName);
                                    }
                                });
                        }
                });
    }

    @Override
    public Mono<Void> fetchCoordinatesForStation(String stationName, boolean overwrite) {
        return repository.findById(stationName)
            .flatMap(entity -> fetchCoordinates(stationName)
                    .flatMap(coords -> {
                        if (!coords.isEmpty()) {
                            if (overwrite || (entity.getLatitude() == null && entity.getLongitude() == null)) {
                                entity.setLatitude(coords.getLatitude());
                                entity.setLongitude(coords.getLongitude());
                                return repository.save(entity).then(); // save only if updated
                            }
                        }
                        return Mono.empty(); // no need to save if nothing changed
                    })
            );
    }

    private Mono<GeocodingResponse> fetchCoordinates(String stationName) {
        Mono<GeocodingResponse> responseMono = registry.waitForCoordinates(stationName);
        messageSender.sendMessage("geocodingDataRequests-out-0", constructRequestEvent(stationName));
        return responseMono;
    }

    private CrudEvent<String, GeocodingRequest> constructRequestEvent(String stationName) {
        GeocodingRequest geocodingRequest = new GeocodingRequest(stationName);
        return new CrudEvent<String, GeocodingRequest>(CrudEvent.Type.GET, stationName, geocodingRequest);
    }
}
