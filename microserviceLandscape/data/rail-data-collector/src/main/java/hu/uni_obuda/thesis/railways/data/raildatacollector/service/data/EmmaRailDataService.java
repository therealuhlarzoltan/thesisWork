package hu.uni_obuda.thesis.railways.data.raildatacollector.service.data;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface EmmaRailDataService {
    Flux<DelayInfo> getDelayInfo(String trainId, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date);
    Flux<TrainRouteResponse> planRoute(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date);
}
