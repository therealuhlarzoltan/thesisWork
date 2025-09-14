package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.route.dto.RouteResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface RailDataService {
    default Flux<DelayInfo> getDelayInfo(String trainNumber, String from, String to, LocalDate date) {
        return Flux.error(new UnsupportedOperationException("Not implemented"));
    }
    default Flux<DelayInfo> getDelayInfo(String trainId, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return Flux.error(new UnsupportedOperationException("Not implemented"));
    }
    default Flux<TrainRouteResponse> planRoute(String from, String to, LocalDate date) {
        return Flux.error(new UnsupportedOperationException("Not implemented"));
    };
    default Flux<TrainRouteResponse> planRoute(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        return Flux.error(new UnsupportedOperationException("Not implemented yet"));
    }
}
