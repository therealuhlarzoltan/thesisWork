package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import hu.uni_obuda.thesis.railways.data.event.Event;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

public interface DelayInfoProcessor extends Consumer<Message<Event<?, ?>>> {
}
