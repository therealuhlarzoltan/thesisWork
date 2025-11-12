package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.common;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.StationDataGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.StationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Primary
@Service
public class ReactiveHttpStationService implements StationService {

    private final StationDataGateway gateway;

    public ReactiveHttpStationService(@Qualifier("reactiveStationDataGateway") StationDataGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public Mono<TrainStationResponse> getStation(String stationCode) {
        return gateway.getTrainStation(stationCode);
    }

    @Override
    public Mono<TrainRouteResponse> getRoute(String trainNumber) {
        return gateway.getTrainRoute(trainNumber);
    }
}
