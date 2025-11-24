package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.registry.CoordinatesRegistry;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

@Slf4j
@RequiredArgsConstructor
public class CoordinateProcessorImpl implements CoordinateProcessor {

    private final ObjectMapper objectMapper;
    private final CoordinatesRegistry registry;
    private final IncomingMessageSink messageSink;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        log.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        if (eventMessage.getHeaders().containsKey("correlationId")) {
            processMessageWithCorrelationId(eventMessage);
        } else {
            processMessageWithoutCorrelationId(eventMessage);
        }
    }

    private void processMessageWithoutCorrelationId(Message<Event<?, ?>> message) {
        log.info("Processing message created at {} with no correlationId...", message.getPayload().getEventCreatedAt());
        HttpResponseEvent responseEvent = retrieveHttpResponseEvent(message.getPayload());
        if (responseEvent == null) {
            log.error("Could not retrieve response event from message: {}", message.getPayload());
            return;
        }
        HttpResponseEvent.Type eventType = responseEvent.getEventType();
        switch (eventType) {
            case SUCCESS -> {
                GeocodingResponse response = retrieveGeocodingResponse(responseEvent);
                if (response == null) {
                    log.error("Could not retrieve geocoding response from event: {}", responseEvent);
                    return;
                }
                messageSink.getCoordinatesSink().tryEmitNext(response);
                registry.onCoordinates(response);
            }
            case ERROR -> {
                log.error("Received an error response: {}", retrieveErrorMessage(responseEvent));
            }
            case null, default -> log.error("Received unknown event type: {}", eventType);
        }

    }

    private void processMessageWithCorrelationId(Message<Event<?, ?>> message) {
        String correlationId = message.getHeaders().get("correlationId").toString();
        log.info("Processing message created at {} with correlationId {}...", message.getPayload().getEventCreatedAt(), correlationId);
        HttpResponseEvent responseEvent = retrieveHttpResponseEvent(message.getPayload());
        if (responseEvent == null) {
            log.error("Could not retrieve response event from message: {}", message.getPayload());
            return;
        }
        HttpResponseEvent.Type eventType = responseEvent.getEventType();
        switch (eventType) {
            case SUCCESS -> {
                GeocodingResponse response = retrieveGeocodingResponse(responseEvent);
                if (response == null) {
                    log.error("Could not retrieve weather info from event: {}", responseEvent);
                    return;
                }
                messageSink.getCoordinatesSink().tryEmitNext(response);
                registry.onCoordinatesWithCorrelationId(correlationId, response);
            }
            case ERROR -> {
                log.error("Received an error response: {}", retrieveErrorMessage(responseEvent));
            }
            case null, default -> log.error("Received unknown event type: {}", eventType);
        }
    }

    private HttpResponseEvent retrieveHttpResponseEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof HttpResponseEvent responseEvent)) {
            log.error("Unexpected event parameters, expected a HttpResponseEvent");
            return null;
        }
        return responseEvent;
    }

    private GeocodingResponse retrieveGeocodingResponse(HttpResponseEvent httpResponseEvent) {
        return deserializeObject(httpResponseEvent.getData().getMessage(), GeocodingResponse.class);
    }

    private String retrieveErrorMessage(HttpResponseEvent httpResponseEvent) {
        if (httpResponseEvent.getEventType() == HttpResponseEvent.Type.ERROR && httpResponseEvent.getData().getMessage() != null) {
            Exception ex = deserializeObject(httpResponseEvent.getData().getMessage(), Exception.class);
            return ex != null ? ex.getMessage() : httpResponseEvent.getData().getMessage();
        } else {
            return "Unexpected error response";
        }
    }

    private <T> T deserializeObject(String json, Class<T> clazz) {
        T obj = null;
        try {
            obj = objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            log.error("Could not deserialize object from json", ex);
            return null;
        }
        return obj;
    }
}
