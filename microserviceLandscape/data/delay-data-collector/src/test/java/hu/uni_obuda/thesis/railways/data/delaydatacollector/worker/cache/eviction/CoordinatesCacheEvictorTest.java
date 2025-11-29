package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.cache.eviction;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.CoordinatesCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainStationRepository;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainStationEntity;

@ExtendWith(MockitoExtension.class)
class CoordinatesCacheEvictorTest {

    @Mock
    private CoordinatesCache coordinatesCache;
    @Mock
    private TrainStationRepository trainStationRepository;
    @Mock
    private TrainStationEntity stationEntity;

    @InjectMocks
    private CoordinatesCacheEvictor testedObject;


    private GeocodingResponse createCoordinates(String address, double lat, double lon) {
        return GeocodingResponse.builder()
                .address(address)
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    @Test
    void evictAndSave_noCoordinates_doesNothingBeyondGetAll() {
        when(coordinatesCache.getAll()).thenReturn(Flux.empty());

        testedObject.evictAndSave();

        verify(coordinatesCache).getAll();
        verifyNoInteractions(trainStationRepository);
        verify(coordinatesCache, never()).evict(anyString());
    }

    @Test
    void evictAndSave_stationNotFound_onlyEvicts() {
        String address = "Budapest-Keleti";
        GeocodingResponse coordinates = createCoordinates(address, 47.5, 19.08);

        when(coordinatesCache.getAll()).thenReturn(Flux.just(coordinates));
        when(trainStationRepository.findById(address)).thenReturn(Mono.empty());
        when(coordinatesCache.evict(address)).thenReturn(Mono.empty());

        testedObject.evictAndSave();

        verify(coordinatesCache).getAll();
        verify(trainStationRepository).findById(address);
        verify(trainStationRepository, never()).save(any());
        verify(coordinatesCache).evict(address);
    }

    @Test
    void evictAndSave_stationWithoutCoordinates_updatesSavesAndEvicts() {
        String address = "Budapest-Keleti";
        double lat = 47.5;
        double lon = 19.08;
        GeocodingResponse coordinates = createCoordinates(address, lat, lon);

        when(coordinatesCache.getAll()).thenReturn(Flux.just(coordinates));
        when(trainStationRepository.findById(address)).thenReturn(Mono.just(stationEntity));

        // short-circuiting condition to avoid UnnecessaryStubbingException
        when(stationEntity.getLatitude()).thenReturn(null);

        when(trainStationRepository.save(stationEntity)).thenReturn(Mono.just(stationEntity));
        when(coordinatesCache.evict(address)).thenReturn(Mono.empty());

        testedObject.evictAndSave();

        verify(coordinatesCache).getAll();
        verify(trainStationRepository).findById(address);
        verify(stationEntity).setLatitude(lat);
        verify(stationEntity).setLongitude(lon);
        verify(trainStationRepository).save(stationEntity);
        verify(coordinatesCache).evict(address);
    }


    @Test
    void evictAndSave_stationWithCoordinates_savesNothingButEvicts() {
        String address = "Budapest-Keleti";
        GeocodingResponse coordinates = createCoordinates(address, 47.5, 19.08);

        when(coordinatesCache.getAll()).thenReturn(Flux.just(coordinates));
        when(trainStationRepository.findById(address)).thenReturn(Mono.just(stationEntity));
        when(stationEntity.getLatitude()).thenReturn(47.0);
        when(stationEntity.getLongitude()).thenReturn(19.0);
        when(coordinatesCache.evict(address)).thenReturn(Mono.empty());

        testedObject.evictAndSave();

        verify(coordinatesCache).getAll();
        verify(trainStationRepository).findById(address);

        verify(stationEntity, never()).setLatitude(any());
        verify(stationEntity, never()).setLongitude(any());
        verify(trainStationRepository, never()).save(any());

        verify(coordinatesCache).evict(address);
    }
}
