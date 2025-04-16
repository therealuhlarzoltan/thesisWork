package hu.uni_obuda.thesis.railways.data.delaydatacollector.repository;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.DelayEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface DelayRepository extends R2dbcRepository<DelayEntity, Long> {
}
