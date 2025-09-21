package hu.uni_obuda.thesis.railways.data.raildatacollector.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.raildatacollector.controller.EmmaRailDataCollector;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfoRequest;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class EmmaMessageProcessorImpl implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(EmmaMessageProcessorImpl.class);

    private final ObjectMapper objectMapper;
    private final EmmaRailDataCollector emmaRailDataCollector;
    private final ResponseMessageSender responseSender;
    private final Scheduler messageProcessingScheduler;

    public EmmaMessageProcessorImpl(ObjectMapper objectMapper, EmmaRailDataCollector emmaRailDataCollector, ResponseMessageSender responseSender, Scheduler messageProcessingScheduler) {
        this.objectMapper = objectMapper;
        this.emmaRailDataCollector = emmaRailDataCollector;
        this.responseSender = responseSender;
        this.messageProcessingScheduler = messageProcessingScheduler;
    }

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        LOG.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        if (eventMessage.getHeaders().containsKey("correlationId")) {
            processMessageWithCorrelationId(eventMessage);
        } else {
            processMessageWithoutCorrelationId(eventMessage);
        }
    }

    private CrudEvent<String, DelayInfoRequest> retrieveCrudEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof CrudEvent<?, ?> crudEvent)) {
            LOG.error("Unexpected event parameters, expected a CrudEvent");
            return null;
        }

        if (!(crudEvent.getKey() instanceof String)) {
            LOG.error("Unexpected event parameters, expected a CrudEvent<String, DelayInfoRequest>");
            return null;
        }

        DelayInfoRequest delayInfoRequest;
        try {
            delayInfoRequest = objectMapper.convertValue(crudEvent.getData(), DelayInfoRequest.class);
        } catch (IllegalArgumentException e) {
            LOG.error("Unexpected event parameters, expected a CrudEvent<String, DelayInfoRequest>");
            return null;
        }

        return new CrudEvent<>(crudEvent.getEventType(), (String)crudEvent.getKey(), delayInfoRequest);
    }

    private void processMessageWithoutCorrelationId(Message<Event<?, ?>> message) {
        LOG.info("Processing message created at {} with no correlationId...", message.getPayload().getEventCreatedAt());
        CrudEvent<String, DelayInfoRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload(), null);
            return;
        }
        CrudEvent.Type eventType = crudEvent.getEventType();
        switch (eventType) {
            case GET -> {
                DelayInfoRequest request = crudEvent.getData();
                Flux<DelayInfo> delayInfoFlux = emmaRailDataCollector.getDelayInfo(request.getTrainNumber(), request.getFrom(), request.getFromLatitude(), request.getFromLongitude(), request.getTo(), request.getToLatitude(), request.getToLongitude(), request.getDate());
                delayInfoFlux
                        .map(delayInfo -> {
                            ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(delayInfo), HttpStatus.OK);
                            return new HttpResponseEvent(HttpResponseEvent.Type.SUCCESS, request.getTrainNumber(), responsePayload);
                        })
                        .doOnNext(event -> responseSender.sendResponseMessage("railDataResponses-out-0", event))
                        .doOnError((throwable) -> {
                            LOG.error("An error occurred: {}", throwable.getMessage());
                            LOG.warn("Sending error response message to delay data collector...");
                            ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(resolveException(throwable)), resolveHttpStatus(resolveException(throwable)));
                            responseSender.sendResponseMessage("railDataResponses-out-0", new HttpResponseEvent(HttpResponseEvent.Type.ERROR, request.getTrainNumber(), responsePayload));
                        })
                        .onErrorResume(throwable -> Mono.empty())
                        .subscribeOn(messageProcessingScheduler)
                        .subscribe();
            }
            case null, default -> handleIncorrectEventTypeError(crudEvent, null);
        }

    }

    private void processMessageWithCorrelationId(Message<Event<?, ?>> message) {
        String correlationId = message.getHeaders().get("correlationId").toString();
        LOG.info("Processing message created at {} with correlationId {}...", message.getPayload().getEventCreatedAt(), correlationId);
        CrudEvent<String, DelayInfoRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload(), correlationId);
            return;
        }
        CrudEvent.Type eventType = crudEvent.getEventType();
        switch (eventType) {
            case GET -> {
                DelayInfoRequest request = crudEvent.getData();
                Flux<DelayInfo> delayInfoFlux = emmaRailDataCollector.getDelayInfo(request.getTrainNumber(), request.getFrom(), request.getFromLatitude(), request.getFromLongitude(), request.getTo(), request.getToLatitude(), request.getToLongitude(), request.getDate());
                delayInfoFlux
                        .map(delayInfo -> {
                            ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(delayInfo), HttpStatus.OK);
                            return new HttpResponseEvent(HttpResponseEvent.Type.SUCCESS, request.getTrainNumber(), responsePayload);
                        })
                        .doOnNext(event -> responseSender.sendResponseMessage("railDataResponses-out-0", correlationId, event))
                        .doOnError(throwable -> {
                            LOG.error("Skipped DelayInfo due to error: {}", throwable.getMessage());
                            LOG.warn("Sending error response message to delay data collector...");
                            ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(resolveException(throwable)), resolveHttpStatus(resolveException(throwable)));
                            responseSender.sendResponseMessage("railDataResponses-out-0", correlationId, new HttpResponseEvent(HttpResponseEvent.Type.ERROR, request.getTrainNumber(), responsePayload));
                        })
                        .onErrorResume(throwable -> Mono.empty())
                        .subscribeOn(messageProcessingScheduler)
                        .subscribe();
            }
            case null, default -> handleIncorrectEventTypeError(crudEvent, correlationId);
        }
    }

    private String serializeObjectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    private void handleIncorrectEventParametersError(Event<?, ?> event, String correlationId) {
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received Event was not an instance of CrudEvent<String, DelayInfoRequest>")), HttpStatus.BAD_REQUEST);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, event.getKey().toString(), responsePayload);
        if (correlationId != null) {
            responseSender.sendResponseMessage("railDataResponses-out-0", correlationId, errorEvent);
        } else {
            responseSender.sendResponseMessage("railDataResponses-out-0", errorEvent);
        }
    }

    private void handleIncorrectEventTypeError(CrudEvent<String, DelayInfoRequest> crudEvent, String correlationId) {
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received event had an unsupported event type")), HttpStatus.METHOD_NOT_ALLOWED);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, crudEvent.getKey(), responsePayload);
        if (correlationId != null) {
            responseSender.sendResponseMessage("railDataResponses-out-0", correlationId, errorEvent);
        } else {
            responseSender.sendResponseMessage("railDataResponses-out-0", errorEvent);
        }
    }

    private Throwable resolveException(Throwable throwable) {
        return switch (throwable) {
            case InvalidInputDataException invalidInputDataException -> invalidInputDataException;
            case ExternalApiException externalApiException -> externalApiException;
            case ExternalApiFormatMismatchException externalApiFormatMismatchException -> externalApiFormatMismatchException;
            case InternalApiException internalApiException -> internalApiException;
            case ApiException apiException -> apiException;
            case TrainNotInServiceException trainNotInServiceException -> trainNotInServiceException;
            default -> throwable;
        };
    }

    private HttpStatus resolveHttpStatus(Throwable throwable) {
        return switch (throwable) {
            case InvalidInputDataException invalidInputDataException -> HttpStatus.resolve(invalidInputDataException.getStatusCode().value());
            case ExternalApiException externalApiException -> HttpStatus.resolve(externalApiException.getStatusCode().value());
            case ExternalApiFormatMismatchException _ -> HttpStatus.BAD_GATEWAY;
            case InternalApiException _ -> HttpStatus.INTERNAL_SERVER_ERROR;
            case ApiException _ -> HttpStatus.BAD_GATEWAY;
            case TrainNotInServiceException _ -> HttpStatus.TOO_EARLY;
            case null, default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
