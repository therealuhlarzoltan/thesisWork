package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.processors;

import hu.uni_obuda.thesis.railways.data.event.Event;
import org.springframework.messaging.Message;

public class GeocodingResponseProcessorImpl implements GeocodingResponseProcessor {
    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {

    }
}
