package hu.uni_obuda.thesis.railways.route.routeplannerservice.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.mapper.RouteResponseMapper;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Primary
@Service
public class ReactiveHttpRoutePlannerService implements RoutePlannerService {

    private static final Map<Character, Character> stationCodeMapping = Map.of(
            'õ', 'ő',
            'û', 'ű'
    );

    private final TimetableService timetableService;
    private final StationService stationService;
    private final WeatherService weatherService;
    private final PredictionService predictionService;
    private final RouteResponseMapper mapper;

    @Override
    public Flux<RouteResponse> planRoute(@NonNull String from, @NonNull String to, @NonNull LocalDateTime dateTime) {
       if (from.isBlank()) {
           return Flux.error(new InvalidInputDataException("from.empty"));
       }
       if (to.isBlank()) {
           return Flux.error(new InvalidInputDataException("to.empty"));
       }
       if (dateTime.isBefore(LocalDateTime.now())) {
           return Flux.error(new InvalidInputDataException("date.before.now"));
       }

        String adjustedFrom = adjustStationCodeFormat(from);
        String adjustedTo = adjustStationCodeFormat(to);

        Mono<TrainStationResponse> fromMono = stationService.getStation(adjustedFrom);
        Mono<TrainStationResponse> toMono = stationService.getStation(adjustedTo);

        return Mono.zip(fromMono, toMono)
                .flatMapMany(tuple -> {
                    TrainStationResponse fromStation = tuple.getT1();
                    TrainStationResponse toStation = tuple.getT2();

                    return timetableService.(fromStation, toStation, dateTime)
                            .flatMap(train -> {
                                // Fetch weather forecasts at from/to locations
                                Mono<WeatherInfo> departureWeather = weatherService.getWeather(fromStation.getLatitude(), fromStation.getLongitude(), dateTime);
                                Mono<WeatherInfo> arrivalWeather = weatherService.getWeather(toStation.getLatitude(), toStation.getLongitude(), dateTime.plusMinutes(train.getDurationMinutes()));

                                // Use weather to predict delays
                                return Mono.zip(departureWeather, arrivalWeather)
                                        .flatMap(weatherTuple -> {
                                            Mono<Prediction> departurePrediction = predictionService.predictDelay(train, weatherTuple.getT1(), true);
                                            Mono<Prediction> arrivalPrediction = predictionService.predictDelay(train, weatherTuple.getT2(), false);

                                            return Mono.zip(departurePrediction, arrivalPrediction)
                                                    .map(delayTuple -> mapper.mapToResponse(
                                                            train,
                                                            fromStation,
                                                            toStation,
                                                            delayTuple.getT1(), // departure delay
                                                            delayTuple.getT2()  // arrival delay
                                                    ));
                                        });
                            });
                });
    }

    private String adjustStationCodeFormat(@NonNull String stationCode) {
        for (int i = 0; i < stationCode.length(); i++) {
            if (stationCodeMapping.containsKey(stationCode.charAt(i))) {
                stationCode = stationCode.replace(stationCode.charAt(i), stationCodeMapping.get(stationCode.charAt(i)));
            }
        }
        return stationCode;
    }
}
