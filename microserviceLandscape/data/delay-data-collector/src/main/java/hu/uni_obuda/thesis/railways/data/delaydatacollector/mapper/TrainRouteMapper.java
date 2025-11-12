package hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainRouteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface TrainRouteMapper {
    @Mappings({
            @Mapping(target = "startStation", source = "from"),
            @Mapping(target = "endStation", source = "to"),
    })
    TrainRouteResponse entityToApi (TrainRouteEntity entity);

    @Mappings({
            @Mapping(target = "from", source = "startStation"),
            @Mapping(target = "to", source = "endStation"),
    })
    TrainRouteEntity apiToEntity(TrainRouteRequest request);
}
