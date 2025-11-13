package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.emma;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionRequest;
import hu.uni_obuda.thesis.railways.model.dto.DelayPredictionResponse;
import hu.uni_obuda.thesis.railways.model.dto.WeatherInfoSnakeCase;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.helper.TimetableProcessingHelper;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.service.*;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

import static hu.uni_obuda.thesis.railways.route.routeplannerservice.util.constant.Constants.STATION_CODE_MAPPING;

@Profile("data-source-emma")
@Primary
@Slf4j
@Service
public class ReactiveHttpEmmaRoutePlannerService implements RoutePlannerService {

    private final EmmaTimetableService timetableService;
    private final GeocodingService geocodingService;
    private final StationService stationService;
    private final WeatherService weatherService;
    private final PredictionService predictionService;
    private final TimetableProcessingHelper helper;

    public ReactiveHttpEmmaRoutePlannerService(@Qualifier("reactiveHttpEmmaTimetableService") EmmaTimetableService timetableService,
                                                 @Qualifier("reactiveHttpGeocodingService") GeocodingService geocodingService,
                                                 @Qualifier("reactiveHttpStationService") StationService stationService,
                                                 @Qualifier("reactiveHttpWeatherService") WeatherService weatherService,
                                                 @Qualifier("reactiveHttpPredictionService") PredictionService predictionService,
                                                 TimetableProcessingHelper helper) {
        this.timetableService = timetableService;
        this.geocodingService = geocodingService;
        this.stationService = stationService;
        this.weatherService = weatherService;
        this.predictionService = predictionService;
        this.helper = helper;
    }

    @Override
    public Flux<RouteResponse> planRoute(@NonNull String from, @NonNull String to, LocalDateTime departureTime, LocalDateTime arrivalTime, Integer maxChanges) {
        if (from.isBlank()) {
            log.error("From station is blank");
            return Flux.error(new InvalidInputDataException("from.empty"));
        }
        if (to.isBlank()) {
            log.error("To station is blank");
            return Flux.error(new InvalidInputDataException("to.empty"));
        }
        if (departureTime == null && arrivalTime == null) {
            log.error("Nor arrival or departure time provided");
            return Flux.error(new InvalidInputDataException("date.missing"));
        }
        if (arrivalTime != null && arrivalTime.isBefore(LocalDateTime.now())) {
            log.error("Arrival date was before now()");
            return Flux.error(new InvalidInputDataException("arrival.date.before.now"));
        }
        if (arrivalTime != null && departureTime != null && arrivalTime.isBefore(departureTime)) {
            log.error("Arrival date was before departure date");
            return Flux.error(new InvalidInputDataException("arrival.date.before.departure"));
        }
        if (maxChanges != null && maxChanges < 0) {
            log.error("Max changes were negative");
            return Flux.error(new InvalidInputDataException("changes.negative"));
        }

        Mono<GeocodingResponse> fromCoordinatesMono = geocodingService.getCoordinates(from);
        Mono<GeocodingResponse> toCoordinatesMono = geocodingService.getCoordinates(to);

        String adjustedFrom = adjustStationCodeFormat(from);
        String adjustedTo = adjustStationCodeFormat(to);
        log.debug("Adjusted from station to {}", adjustedFrom);
        log.debug("Adjusted to station to {}", adjustedTo);


        log.info("Getting possible routes from {} to {} with departure time {} and arrival time {} and max changes {}", from, to, departureTime, arrivalTime, maxChanges);
        return Mono.zip(fromCoordinatesMono, toCoordinatesMono)
                .flatMapMany(coordinatesTuple ->
                    timetableService
                            .getTimetable(from, coordinatesTuple.getT1().getLatitude(), coordinatesTuple.getT1().getLongitude(),
                                    to, coordinatesTuple.getT2().getLatitude(), coordinatesTuple.getT2().getLongitude(),
                                    departureTime != null ? departureTime.toLocalDate() : arrivalTime.toLocalDate())
                        .transform(flux -> {
                            if (departureTime != null)
                                return helper.filterByDeparture(departureTime, flux);
                            return flux;
                        })
                        .transform(flux -> {
                            if (arrivalTime != null)
                                return helper.filterByArrival(arrivalTime, flux);
                            return flux;
                        })
                        .transform(flux -> {
                            if (maxChanges != null)
                                return helper.filterByChanges(maxChanges, flux);
                            return flux;
                        })
                )
                .flatMap(route -> Flux.fromIterable(route.getTrains())
                        .flatMap(train -> {
                            boolean hasActuals = train.getFromTimeActual() != null && !train.getFromTimeActual().isBlank() && train.getToTimeActual() != null && !train.getToTimeActual().isBlank();
                            if (hasActuals) {
                                log.info("Train {} is already en route, not attempting to make predictions", train.getTrainNumber());
                                return Mono.just(RouteResponse.Train.builder()
                                        .trainNumber(train.getTrainNumber())
                                        .lineNumber(train.getLineNumber())
                                        .fromStation(train.getFromStation())
                                        .toStation(train.getToStation())
                                        .fromTimeScheduled(train.getFromTimeScheduled())
                                        .toTimeScheduled(train.getToTimeScheduled())
                                        .fromTimeActual(train.getFromTimeActual())
                                        .toTimeActual(train.getToTimeActual())
                                        .build());
                            }
                            return stationService.getRoute(train.getTrainNumber())
                                    .filter(Objects::nonNull)
                                    .flatMapMany(routeInfo -> Flux.zip(
                                            stationService.getStation(routeInfo.getStartStation()),
                                            stationService.getStation(routeInfo.getEndStation())
                                    ))
                                    .filter(tuple -> tuple.getT1() != null && tuple.getT2() != null &&
                                            tuple.getT1().getLatitude() != null && tuple.getT2().getLatitude() != null)
                                    .flatMap(tuple -> {
                                        TrainStationResponse fromStation = tuple.getT1();
                                        TrainStationResponse toStation = tuple.getT2();
                                        log.info("Retrieved coordinates for station {}: ({}, {})", fromStation.getStationCode(), fromStation.getLatitude(), fromStation.getLongitude());
                                        log.info("Retrieved coordinates for station {}: ({}, {})", toStation.getStationCode(), toStation.getLatitude(), toStation.getLongitude());

                                        log.info("Attempting to get weatherInfos for stations {} and {}", fromStation.getStationCode(), toStation.getStationCode());
                                        Mono<WeatherInfo> fromWeather = weatherService.getWeather(fromStation.getStationCode(), fromStation.getLatitude(), fromStation.getLongitude(), parseTime(train.getFromTimeScheduled()));
                                        Mono<WeatherInfo> toWeather = weatherService.getWeather(toStation.getStationCode(), toStation.getLatitude(), toStation.getLongitude(), parseTime(train.getToTimeScheduled()));
                                        log.info("Got weatherInfos for stations {} and {}", fromStation.getStationCode(), toStation.getStationCode());

                                        log.info("Attempting to make predictions...");
                                        Mono<DelayPredictionResponse> fromDelay = fromWeather.flatMap(weather -> predictionService.predictDepartureDelay(
                                                DelayPredictionRequest.builder()
                                                        .stationCode(fromStation.getStationCode())
                                                        .trainNumber(train.getTrainNumber())
                                                        .stationLatitude(fromStation.getLatitude())
                                                        .stationLongitude(fromStation.getLongitude())
                                                        .scheduledDeparture(parseTime(train.getFromTimeScheduled()))
                                                        .date(parseTime(train.getFromTimeScheduled()).toLocalDate())
                                                        .weatherWrapper(new WeatherInfoSnakeCase(weather))
                                                        .build()
                                        ));

                                        Mono<DelayPredictionResponse> toDelay = toWeather.flatMap(weather -> predictionService.predictArrivalDelay(
                                                DelayPredictionRequest.builder()
                                                        .stationCode(toStation.getStationCode())
                                                        .trainNumber(train.getTrainNumber())
                                                        .stationLatitude(toStation.getLatitude())
                                                        .stationLongitude(toStation.getLongitude())
                                                        .scheduledArrival(parseTime(train.getToTimeScheduled()))
                                                        .date(parseTime(train.getToTimeScheduled()).toLocalDate())
                                                        .weatherWrapper(new WeatherInfoSnakeCase(weather))
                                                        .build()
                                        ));

                                        return Mono.zip(fromDelay, toDelay).map(delays -> RouteResponse.Train.builder()
                                                .trainNumber(train.getTrainNumber())
                                                .lineNumber(train.getLineNumber())
                                                .fromStation(train.getFromStation())
                                                .toStation(train.getToStation())
                                                .fromTimeScheduled(train.getFromTimeScheduled())
                                                .toTimeScheduled(train.getToTimeScheduled())
                                                .fromTimePredicted(addDelay(train.getFromTimeScheduled(), delays.getT1().getPredictedDelay()))
                                                .toTimePredicted(addDelay(train.getToTimeScheduled(), delays.getT2().getPredictedDelay()))
                                                .build());
                                    })
                                    .switchIfEmpty(Mono.fromCallable(() -> {
                                        if (hasActuals) {
                                            log.warn("Train {} is already enroute, but not found in database, not attempting to make predictions", train.getTrainNumber());
                                            return RouteResponse.Train.builder()
                                                    .trainNumber(train.getTrainNumber())
                                                    .lineNumber(train.getLineNumber())
                                                    .fromStation(train.getFromStation())
                                                    .toStation(train.getToStation())
                                                    .fromTimeScheduled(train.getFromTimeScheduled())
                                                    .toTimeScheduled(train.getToTimeScheduled())
                                                    .fromTimeActual(train.getFromTimeActual())
                                                    .toTimeActual(train.getToTimeActual())
                                                    .build();
                                        } else {
                                            log.warn("Train {} is not en route, but not found in database, not attempting to make predictions", train.getTrainNumber());
                                            return RouteResponse.Train.builder()
                                                    .trainNumber(train.getTrainNumber())
                                                    .lineNumber(train.getLineNumber())
                                                    .fromStation(train.getFromStation())
                                                    .toStation(train.getToStation())
                                                    .fromTimeScheduled(train.getFromTimeScheduled())
                                                    .toTimeScheduled(train.getToTimeScheduled())
                                                    .build();
                                        }
                                    }));
                        })
                        .collectList()
                        .map(trains -> RouteResponse.builder().trains(trains).build())
                );
    }


    private String adjustStationCodeFormat(@NonNull String stationCode) {
        for (int i = 0; i < stationCode.length(); i++) {
            if (STATION_CODE_MAPPING.containsKey(stationCode.charAt(i))) {
                stationCode = stationCode.replace(stationCode.charAt(i), STATION_CODE_MAPPING.get(stationCode.charAt(i)));
            }
        }
        return stationCode;
    }

    private LocalDateTime parseTime(String timeStr) {
        return LocalDateTime.parse(timeStr);
    }

    private String addDelay(String timeStr, Double delayMinutes) {
        return parseTime(timeStr).plusMinutes(delayMinutes.longValue()).toString();
    }
}
