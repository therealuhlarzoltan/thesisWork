package hu.uni_obuda.thesis.railways.data.raildatacollector.service;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.RailDelayWebClient;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RailDataServoceImpl implements RailDataService {

    private final RailDelayWebClient webClient;

    @Override
    public Flux<DelayInfo> getDelayInfo(String trainNumber, LocalDate date) {
        return null;
    }
}
