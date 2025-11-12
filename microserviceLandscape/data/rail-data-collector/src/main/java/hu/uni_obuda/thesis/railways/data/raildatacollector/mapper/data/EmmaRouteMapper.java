package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Profile("data-source-emma")
@Component
@Slf4j
@RequiredArgsConstructor
public class EmmaRouteMapper {

    public Mono<List<TrainRouteResponse>> mapToRouteResponse(EmmaTimetableResponse timetableResponse, LocalDate date) {
        List<TrainRouteResponse> routes = new ArrayList<>();


        return Mono.just(routes);
    }
}
