package hu.uni_obuda.thesis.railways.data.weatherdatacollector.workers;

import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;

public interface ResponseMessageSender {
    void sendResponseMessage(String bindingName, HttpResponseEvent event);
}
