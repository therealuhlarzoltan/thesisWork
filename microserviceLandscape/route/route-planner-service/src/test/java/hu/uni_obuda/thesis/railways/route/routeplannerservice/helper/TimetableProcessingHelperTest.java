package hu.uni_obuda.thesis.railways.route.routeplannerservice.helper;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

class TimetableProcessingHelperTest {

    private final TimetableProcessingHelper helper = new TimetableProcessingHelper();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Test
    void filterByDeparture_includesOnlyRoutesStartingAfterRequestedTime() {
        LocalDateTime requestedDeparture = LocalDateTime.of(2025, 1, 1, 8, 0);

        TrainRouteResponse matchingRoute = routeWithSingleTrain(
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 1, 1, 11, 0)
        );

        TrainRouteResponse earlyRoute = routeWithSingleTrain(
                LocalDateTime.of(2025, 1, 1, 7, 0),
                LocalDateTime.of(2025, 1, 1, 8, 0)
        );

        TrainRouteResponse routeWithNoTrains = TrainRouteResponse.builder()
                .trains(Collections.emptyList())
                .build();

        Flux<TrainRouteResponse> source = Flux.just(matchingRoute, earlyRoute, routeWithNoTrains);

        StepVerifier.create(helper.filterByDeparture(requestedDeparture, source))
                .expectNext(matchingRoute)
                .verifyComplete();
    }

    @Test
    void filterByArrival_includesOnlyRoutesEndingBeforeRequestedArrivalTime() {
        LocalDateTime requestedArrival = LocalDateTime.of(2025, 1, 1, 15, 0);

        TrainRouteResponse matchingRoute = routeWithSingleTrain(
                LocalDateTime.of(2025, 1, 1, 12, 0),
                LocalDateTime.of(2025, 1, 1, 14, 0)
        );

        TrainRouteResponse lateRoute = routeWithSingleTrain(
                LocalDateTime.of(2025, 1, 1, 13, 0),
                LocalDateTime.of(2025, 1, 1, 16, 0)
        );

        TrainRouteResponse routeWithNoTrains = TrainRouteResponse.builder()
                .trains(Collections.emptyList())
                .build();

        Flux<TrainRouteResponse> source = Flux.just(matchingRoute, lateRoute, routeWithNoTrains);

        StepVerifier.create(helper.filterByArrival(requestedArrival, source))
                .expectNext(matchingRoute)
                .verifyComplete();
    }

    @Test
    void filterByChanges_filtersBasedOnAllowedNumberOfChanges() {
        int allowedChanges = 1;  // so changes + 1 = 2

        TrainRouteResponse oneTrainRoute = TrainRouteResponse.builder()
                .trains(List.of(
                        TrainRouteResponse.Train.builder().trainNumber("R1").build()
                ))
                .build();

        TrainRouteResponse twoTrainRoute = TrainRouteResponse.builder()
                .trains(List.of(
                        TrainRouteResponse.Train.builder().trainNumber("R2-1").build(),
                        TrainRouteResponse.Train.builder().trainNumber("R2-2").build()
                ))
                .build();

        TrainRouteResponse threeTrainRoute = TrainRouteResponse.builder()
                .trains(List.of(
                        TrainRouteResponse.Train.builder().trainNumber("R3-1").build(),
                        TrainRouteResponse.Train.builder().trainNumber("R3-2").build(),
                        TrainRouteResponse.Train.builder().trainNumber("R3-3").build()
                ))
                .build();

        Flux<TrainRouteResponse> source = Flux.just(oneTrainRoute, twoTrainRoute, threeTrainRoute);

        StepVerifier.create(helper.filterByChanges(allowedChanges, source))
                .expectNext(oneTrainRoute)
                .expectNext(twoTrainRoute)
                .verifyComplete();
    }

    @Test
    void filterByDeparture_handlesInvalidDateStringsGracefully() {
        LocalDateTime requestedDeparture = LocalDateTime.of(2025, 1, 1, 8, 0);

        TrainRouteResponse matchingRoute = routeWithSingleTrain(
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 1, 1, 11, 0)
        );

        TrainRouteResponse invalidStartRoute = TrainRouteResponse.builder()
                .trains(List.of(
                        TrainRouteResponse.Train.builder()
                                .fromTimeScheduled("not-a-valid-date")
                                .toTimeScheduled("2025-01-01T11:00:00")
                                .build()
                ))
                .build();

        Flux<TrainRouteResponse> source = Flux.just(matchingRoute, invalidStartRoute);

        StepVerifier.create(helper.filterByDeparture(requestedDeparture, source))
                .expectNext(matchingRoute)
                .verifyComplete();
    }

    private TrainRouteResponse routeWithSingleTrain(LocalDateTime from, LocalDateTime to) {
        TrainRouteResponse.Train train = TrainRouteResponse.Train.builder()
                .fromTimeScheduled(from != null ? from.format(FORMATTER) : null)
                .toTimeScheduled(to != null ? to.format(FORMATTER) : null)
                .build();

        return TrainRouteResponse.builder()
                .trains(List.of(train))
                .build();
    }
}
