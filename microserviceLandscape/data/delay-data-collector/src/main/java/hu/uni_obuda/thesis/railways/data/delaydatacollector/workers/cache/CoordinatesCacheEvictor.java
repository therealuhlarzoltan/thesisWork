package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.cache;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.CoordinatesCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainStationRepository;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class CoordinatesCacheEvictor {

    private static final Logger LOG = LoggerFactory.getLogger(CoordinatesCacheEvictor.class);

    private final CoordinatesCache coordinatesCache;
    private final TrainStationRepository trainStationRepository;

    @ScheduledJob("coordinatesCacheEviction")
    public void evictAndSave() {
        LOG.info("Evicting coordinates cache and saving them to the repository at 3 AM...");
        coordinatesCache.getAll()
                .flatMap(coordinates ->
                        trainStationRepository.findById(coordinates.getAddress())
                                .flatMap(entity -> {
                                    if (entity.getLatitude() == null || entity.getLongitude() == null) {
                                        LOG.info("Updating coordinates for station {}", coordinates.getAddress());
                                        entity.setLatitude(coordinates.getLatitude());
                                        entity.setLongitude(coordinates.getLongitude());
                                        return trainStationRepository.save(entity);
                                    } else {
                                        return Mono.just(entity);
                                    }
                                })
                                .then(coordinatesCache.evict(coordinates.getAddress()))
                )
                .then()
                .subscribe();
    }

}
