package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ScheduledIntervalMapper {

    @Mappings(
            @Mapping(target = "id", ignore = true)
    )
    ScheduledIntervalEntity apiToEntity(ScheduledIntervalRequest api);

    ScheduledIntervalResponse entityToApi(ScheduledIntervalEntity entity);
}

