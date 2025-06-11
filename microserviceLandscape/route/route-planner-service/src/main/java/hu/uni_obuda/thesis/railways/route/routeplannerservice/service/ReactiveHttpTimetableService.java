package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.RailDataGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Slf4j
@Primary
@Service
public class ReactiveHttpTimetableService implements TimetableService {

    private final RailDataGateway railDataGateway;

    @Autowired
    public ReactiveHttpTimetableService(@Qualifier("reactiveRailDataGateway") RailDataGateway railDataGateway) {
        this.railDataGateway = railDataGateway;
    }

    @Override
    public Flux<TrainRouteResponse> getTimetable(String from, String to, LocalDate date) {
        return railDataGateway.getTimetable(from, to, date);
    }
}
