package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.emma;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.RoutePlannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Profile("data-source-emma")
@Primary
@Slf4j
@Service
public class ReactiveHttpEmmaRoutePlannerService implements RoutePlannerService {
    @Override
    public Flux<RouteResponse> planRoute(String from, String to, LocalDateTime departureTime, LocalDateTime arrivalTime, Integer maxChanges) {
        return null;
    }
}
