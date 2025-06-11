package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.RailWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Slf4j
@Primary
@Component
public class ReactiveRailDataGateway implements RailDataGateway {

    private final RailWebClient webClient;

    public ReactiveRailDataGateway(@Qualifier("reactiveRailWebClient") RailWebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Flux<TrainRouteResponse> getTimetable(String from, String to, LocalDate date) {
        return webClient.makeRouteRequest(from, to, date);
    }
}
