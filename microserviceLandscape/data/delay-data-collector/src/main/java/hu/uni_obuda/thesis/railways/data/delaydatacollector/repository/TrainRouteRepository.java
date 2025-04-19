package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.TrainRouteEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface TrainRouteRepository extends R2dbcRepository<TrainRouteEntity, String> {
}
