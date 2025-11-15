package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.emma;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.EmmaRailDataGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.EmmaTimetableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Profile("data-source-emma")
@Primary
@Slf4j
@Service
public class ReactiveHttpEmmaTimetableService implements EmmaTimetableService {

    private final EmmaRailDataGateway railDataGateway;

    @Autowired
    public ReactiveHttpEmmaTimetableService(@Qualifier("reactiveEmmaRailDataGateway") EmmaRailDataGateway elviraRailDataGateway) {
        this.railDataGateway = elviraRailDataGateway;
    }

    @Override
    public Flux<TrainRouteResponse> getTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return railDataGateway.getTimetable(from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date);
    }
}
