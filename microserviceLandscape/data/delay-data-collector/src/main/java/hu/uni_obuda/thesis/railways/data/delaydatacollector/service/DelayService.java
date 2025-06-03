package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.DelayRecord;
import hu.uni_obuda.thesis.railways.data.event.DataTransferEvent;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import reactor.core.publisher.Flux;

public interface DelayService {
    void processDelays(Flux<DelayInfo> delayInfos);
    Flux<DelayInfo> getTrainDelays();
    Flux<DataTransferEvent<DelayRecord>> getBatches(int batchSize, String routingKey);
}
