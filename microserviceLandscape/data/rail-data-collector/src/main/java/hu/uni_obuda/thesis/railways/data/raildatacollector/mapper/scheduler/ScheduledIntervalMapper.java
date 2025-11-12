package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.scheduler;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.hash.UUIDHashFunction;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ScheduledIntervalMapper {

    default ScheduledIntervalEntity apiToEntity(ScheduledIntervalRequest api) {
        Integer id = UUIDHashFunction.apply(UUID.randomUUID());
        return new ScheduledIntervalEntity(id, api.getJobId(), api.getIntervalInMillis());
    }

    ScheduledIntervalResponse entityToApi(ScheduledIntervalEntity entity);
}

