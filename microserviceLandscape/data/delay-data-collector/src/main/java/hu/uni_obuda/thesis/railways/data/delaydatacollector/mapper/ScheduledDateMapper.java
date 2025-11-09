package hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledDateEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduledDateMapper {
    ScheduledDateEntity apiToEntity(ScheduledDateRequest api);
    ScheduledDateResponse entityToApi(ScheduledDateEntity entity);
}
