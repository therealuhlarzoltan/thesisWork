package hu.uni_obuda.thesis.railways.data.raildatacollector.worker;

import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;

public interface ResponseMessageSender {
    void sendResponseMessage(String bindingName, HttpResponseEvent event);
    void sendResponseMessage(String bindingName, String correlationId, HttpResponseEvent event);
}
