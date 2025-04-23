package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.senders;

import hu.uni_obuda.thesis.railways.data.event.Event;

public interface MessageSender {
    void sendMessage(String bindingName, Event<?, ?> event);
    void sendMessage(String bindingName, String correlationId, Event<?, ?> event);
}
