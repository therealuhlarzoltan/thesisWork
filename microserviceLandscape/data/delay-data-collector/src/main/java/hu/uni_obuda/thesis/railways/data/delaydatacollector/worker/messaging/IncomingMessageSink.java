package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.Getter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
@Getter
public class IncomingMessageSink {
    private final Sinks.Many<DelayInfo> delaySink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<WeatherInfo> weatherSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<GeocodingResponse> coordinatesSink = Sinks.many().multicast().onBackpressureBuffer();
}
