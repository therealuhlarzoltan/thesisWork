package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ScheduledDateMapper {

    @Mappings(
            @Mapping(target = "id", ignore = true)
    )
    ScheduledDateEntity apiToEntity(ScheduledDateRequest api);

    ScheduledDateResponse entityToApi(ScheduledDateEntity entity);
}
