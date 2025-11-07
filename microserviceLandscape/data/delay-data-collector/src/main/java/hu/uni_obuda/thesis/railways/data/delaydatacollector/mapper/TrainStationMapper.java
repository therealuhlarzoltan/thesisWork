package hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainStationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrainStationMapper {
    TrainStationEntity apiToEntity(TrainStationRequest trainStationRequest);
    TrainStationResponse entityToApi(TrainStationEntity trainStationEntity);
}
