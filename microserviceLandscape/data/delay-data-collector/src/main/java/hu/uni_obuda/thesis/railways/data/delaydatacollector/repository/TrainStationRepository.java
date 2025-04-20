package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.TrainStationEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface TrainStationRepository extends R2dbcRepository<TrainStationEntity, String> {
}
