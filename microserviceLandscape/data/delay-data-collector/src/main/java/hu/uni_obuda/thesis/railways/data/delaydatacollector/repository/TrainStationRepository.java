package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.TrainRouteEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.TrainStationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;

public interface TrainStationRepository extends R2dbcRepository<TrainStationEntity, String> {
    @Query("""
    INSERT INTO stations (station_code)
    VALUES (:stationCode)
    RETURNING *
    """)
    Mono<TrainStationEntity> insertStation(@Param("stationCode") String stationCode);
    Mono<TrainStationEntity> findByStationCode(@Param("stationCode") String stationCode);
}
