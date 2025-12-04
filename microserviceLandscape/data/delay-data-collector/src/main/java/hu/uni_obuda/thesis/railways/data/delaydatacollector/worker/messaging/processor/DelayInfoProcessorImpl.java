package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;

import hu.uni_obuda.thesis.railways.util.exception.datacollectors.TrainNotInServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;

@Slf4j
@RequiredArgsConstructor
public class DelayInfoProcessorImpl implements DelayInfoProcessor {

    private final ObjectMapper objectMapper;
    private final IncomingMessageSink messageSink;
    private final TrainStatusCache statusCache;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        log.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        processMessage(eventMessage);
    }

    private void processMessage(Message<Event<?, ?>> message) {
        log.info("Processing message created at {}...", message.getPayload().getEventCreatedAt());
        HttpResponseEvent responseEvent = retriveHttpResponseEvent(message.getPayload());
        if (responseEvent == null) {
            log.error("Could not retrieve response event from message: {}", message.getPayload());
            return;
        }
        HttpResponseEvent.Type eventType = responseEvent.getEventType();
        switch (eventType) {
            case SUCCESS -> {
                DelayInfo response = retrieveDelayInfo(responseEvent);
                if (response == null) {
                    log.error("Could not retrieve delay info from event: {}", responseEvent);
                    return;
                }
                var result = messageSink.getDelaySink().tryEmitNext(response);
                if (result.isSuccess())
                    log.info("Added delay info {}, to message sink for train {} and station {}", response, response.getTrainNumber(), response.getStationCode());
                else
                    log.error("Could not add delay info {}, to message sink for train {}", response, response.getTrainNumber());
            }
            case ERROR -> {
                log.error("Received an error response for id: {}", responseEvent.getKey());
                if (responseEvent.getData().getStatus() == HttpStatus.TOO_EARLY) {
                    TrainNotInServiceException trainNotInServiceException = deserializeObject(responseEvent.getData().getMessage(), TrainNotInServiceException.class);
                    if (trainNotInServiceException != null) {
                        log.warn("Train with number {} is not in operation on {}, marking it as complete", trainNotInServiceException.getTrainNumber(), trainNotInServiceException.getDate());
                        statusCache.markComplete(trainNotInServiceException.getTrainNumber(), trainNotInServiceException.getDate()).block();
                    }
                }
            }
            case null, default -> log.error("Received unknown event type: {}", eventType);
        }
    }

    private HttpResponseEvent retriveHttpResponseEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof HttpResponseEvent responseEvent)) {
            log.error("Unexpected event parameters, expected a HttpResponseEvent");
            return null;
        }
        return responseEvent;
    }

    private DelayInfo retrieveDelayInfo(HttpResponseEvent httpResponseEvent) {
        return deserializeObject(httpResponseEvent.getData().getMessage(), DelayInfo.class);
    }

    private <T> T deserializeObject(String json, Class<T> clazz) {
        T obj;
        try {
            obj = objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            log.error("Could not deserialize object from json", ex);
            return null;
        }
        return obj;
    }
}
