package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.StationWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Primary
@Component
public class ReactiveStationDataGateway implements StationDataGateway {

    private final StationWebClient webClient;

    public ReactiveStationDataGateway(@Qualifier("reactiveStationWebClient") StationWebClient webClient) {
        this.webClient = webClient;
    }


    @Override
    public Mono<TrainRouteResponse> getTrainRoute(String trainNumber) {
        return webClient.makeTrainRouteRequest(trainNumber);
    }

    @Override
    public Mono<TrainStationResponse> getTrainStation(String stationCode) {
        return webClient.makeStationRequest(stationCode);
    }
}
