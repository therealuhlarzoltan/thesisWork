package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;

import hu.uni_obuda.thesis.railways.util.exception.datacollectors.TrainNotInServiceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;

@RequiredArgsConstructor
public class DelayInfoProcessorImpl implements DelayInfoProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DelayInfoProcessorImpl.class);

    private final ObjectMapper objectMapper;
    private final IncomingMessageSink messageSink;
    private final TrainStatusCache statusCache;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        LOG.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        if (eventMessage.getHeaders().containsKey("correlationId")) {
            processMessageWithCorrelationId(eventMessage);
        } else {
            processMessageWithoutCorrelationId(eventMessage);
        }
    }

    private void processMessageWithoutCorrelationId(Message<Event<?, ?>> message) {
        LOG.info("Processing message created at {} with no correlationId...", message.getPayload().getEventCreatedAt());
        HttpResponseEvent responseEvent = retriveHttpResponseEvent(message.getPayload());
        if (responseEvent == null) {
            LOG.error("Could not retrieve response event from message: {}", message.getPayload());
            return;
        }
        HttpResponseEvent.Type eventType = responseEvent.getEventType();
        switch (eventType) {
            case SUCCESS -> {
                DelayInfo response = retrieveDelayInfo(responseEvent);
                if (response == null) {
                    LOG.error("Could not retrieve delay info from event: {}", responseEvent);
                    return;
                }
                var result = messageSink.getDelaySink().tryEmitNext(response);
                if (result.isSuccess())
                    LOG.info("Added delay info {}, to message sink for train {} and station {}", response, response.getTrainNumber(), response.getStationCode());
                else
                    LOG.error("Could not add delay info {}, to message sink for train {}", response, response.getTrainNumber());
            }
            case ERROR -> {
                LOG.error("Received an error response for id: {}", responseEvent.getKey());
                if (responseEvent.getData().getStatus() == HttpStatus.TOO_EARLY) {
                    TrainNotInServiceException trainNotInServiceException = deserializeObject(responseEvent.getData().getMessage(), TrainNotInServiceException.class);
                    if (trainNotInServiceException != null) {
                        LOG.warn("Train with number {} is not in operation on {}, marking it as complete", trainNotInServiceException.getTrainNumber(), trainNotInServiceException.getDate());
                        statusCache.markComplete(trainNotInServiceException.getTrainNumber(), trainNotInServiceException.getDate()).block();
                    }
                }
            }
            case null, default -> LOG.error("Received unknown event type: {}", eventType);
        }

    }

    private void processMessageWithCorrelationId(Message<Event<?, ?>> message) {
        String correlationId = message.getHeaders().get("correlationId").toString();
        LOG.info("Processing message created at {} with correlationId {}...", message.getPayload().getEventCreatedAt(), correlationId);
        HttpResponseEvent responseEvent = retriveHttpResponseEvent(message.getPayload());
        if (responseEvent == null) {
            LOG.error("Could not retrieve response event from message: {}", message.getPayload());
            return;
        }
        HttpResponseEvent.Type eventType = responseEvent.getEventType();
        switch (eventType) {
            case SUCCESS -> {
                DelayInfo response = retrieveDelayInfo(responseEvent);
                if (response == null) {
                    LOG.error("Could not retrieve delay info from event: {}", responseEvent);
                    return;
                }
                var result = messageSink.getDelaySink().tryEmitNext(response);
                if (result.isSuccess())
                    LOG.info("Added delay info to message sink for train {} and station {}", response.getTrainNumber(), response.getStationCode());
                else
                    LOG.error("Could not add delay info to message sink for train {} and station {} with correlationId {}", response.getTrainNumber(), response.getStationCode(), correlationId);
            }
            case ERROR -> {
                LOG.error("Received an error response for correlationId: {}", correlationId);
                if (responseEvent.getData().getStatus() == HttpStatus.TOO_EARLY) {
                    TrainNotInServiceException trainNotInServiceException = deserializeObject(responseEvent.getData().getMessage(), TrainNotInServiceException.class);
                    if (trainNotInServiceException != null) {
                        LOG.warn("Train with number {} is not in operation on {}, marking it as complete", trainNotInServiceException.getTrainNumber(), trainNotInServiceException.getDate());
                        statusCache.markComplete(trainNotInServiceException.getTrainNumber(), trainNotInServiceException.getDate()).block();
                    }
                }
            }
            case null, default -> LOG.error("Received unknown event type: {}", eventType);
        }
    }

    private HttpResponseEvent retriveHttpResponseEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof HttpResponseEvent responseEvent)) {
            LOG.error("Unexpected event parameters, expected a HttpResponseEvent");
            return null;
        }
        return responseEvent;
    }

    private DelayInfo retrieveDelayInfo(HttpResponseEvent httpResponseEvent) {
        return deserializeObject(httpResponseEvent.getData().getMessage(), DelayInfo.class);
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
            LOG.error("Could not deserialize object from json", ex);
            return null;
        }
        return obj;
    }


}
