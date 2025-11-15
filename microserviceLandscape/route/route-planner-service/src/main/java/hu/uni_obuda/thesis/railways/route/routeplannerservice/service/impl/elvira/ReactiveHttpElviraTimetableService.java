package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.elvira;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.ElviraRailDataGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.ElviraTimetableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Profile("data-source-elvira")
@Primary
@Service
public class ReactiveHttpElviraTimetableService implements ElviraTimetableService {

    private final ElviraRailDataGateway railDataGateway;

    @Autowired
    public ReactiveHttpElviraTimetableService(@Qualifier("reactiveElviraRailDataGateway") ElviraRailDataGateway elviraRailDataGateway) {
        this.railDataGateway = elviraRailDataGateway;
    }

    @Override
    public Flux<TrainRouteResponse> getTimetable(String from, String to, LocalDate date) {
        return railDataGateway.getTimetable(from, to, date);
    }
}
