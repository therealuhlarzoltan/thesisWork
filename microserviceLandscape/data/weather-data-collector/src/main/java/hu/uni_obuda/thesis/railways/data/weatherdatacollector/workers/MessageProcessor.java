package hu.uni_obuda.thesis.railways.data.weatherdatacollector.workers;

import hu.uni_obuda.thesis.railways.data.event.Event;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

public interface MessageProcessor extends Consumer<Message<Event<?, ?>>> {
}
