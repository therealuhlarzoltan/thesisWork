package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.scheduler;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.hash.UUIDHashFunction;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ScheduledDateMapper {

    default ScheduledDateEntity apiToEntity(ScheduledDateRequest api) {
        Integer id = UUIDHashFunction.apply(UUID.randomUUID());
        return new ScheduledDateEntity(id, api.getJobId(), api.getCronExpression());
    }

    ScheduledDateResponse entityToApi(ScheduledDateEntity entity);
}
