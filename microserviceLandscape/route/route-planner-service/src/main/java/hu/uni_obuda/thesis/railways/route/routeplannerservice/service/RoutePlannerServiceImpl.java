package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.RailDataGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.StationGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.WeatherDataGateway;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.helper.TimetableProcessingHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class RoutePlannerServiceImpl implements RoutePlannerService {

    private final StationGateway stationGateway;
    private final WeatherDataGateway weatherDataGateway;
    private final RailDataGateway railDataGateway;
    private final TimetableProcessingHelper timetableHelper;

    @Override
    public RouteResponse planRoute(String from, String to, LocalDateTime dateTime) {

    }
}
