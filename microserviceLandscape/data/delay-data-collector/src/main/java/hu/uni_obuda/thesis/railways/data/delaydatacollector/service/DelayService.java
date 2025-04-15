package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import reactor.core.publisher.Flux;

public interface DelayService {
    void processDelays(Flux<DelayInfo> delayInfos);
}
