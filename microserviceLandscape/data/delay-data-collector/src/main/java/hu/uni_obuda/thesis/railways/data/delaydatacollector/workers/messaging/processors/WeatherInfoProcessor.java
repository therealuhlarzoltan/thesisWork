package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.processors;

import hu.uni_obuda.thesis.railways.data.event.Event;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

public interface WeatherInfoProcessor extends Consumer<Message<Event<?, ?>>> {
}
