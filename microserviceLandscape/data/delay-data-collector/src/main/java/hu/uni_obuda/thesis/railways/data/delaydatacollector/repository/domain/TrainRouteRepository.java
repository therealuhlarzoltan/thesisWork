package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainRouteEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import org.springframework.data.repository.query.Param;

public interface TrainRouteRepository extends R2dbcRepository<TrainRouteEntity, String> {
    @Query("""
    INSERT INTO trains (train_number, line_number, start_station, end_station)
    VALUES (:trainNumber, :lineNumber, :from, :to)
    RETURNING *
    """)
    Mono<TrainRouteEntity> insertTrain(@Param("trainNumber") String trainNumber,
                                       @Param("lineNumber") String lineNumber,
                                       @Param("from") String from,
                                       @Param("to") String to);
}
