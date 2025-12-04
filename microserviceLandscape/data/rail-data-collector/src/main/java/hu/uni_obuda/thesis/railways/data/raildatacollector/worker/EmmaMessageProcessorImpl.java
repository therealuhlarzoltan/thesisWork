package hu.uni_obuda.thesis.railways.data.raildatacollector.worker;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
@RequiredArgsConstructor
public class EmmaMessageProcessorImpl implements MessageProcessor {

    private final ObjectMapper objectMapper;
    private final EmmaRailDataCollector emmaRailDataCollector;
    private final ResponseMessageSender responseSender;
    private final Scheduler messageProcessingScheduler;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        log.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        processMessage(eventMessage);
    }

    private CrudEvent<String, DelayInfoRequest> retrieveCrudEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof CrudEvent<?, ?> crudEvent)) {
            log.error("Unexpected event parameters, expected a CrudEvent");
            return null;
        }

        if (!(crudEvent.getKey() instanceof String)) {
            log.error("Unexpected event parameters, expected a CrudEvent<String, DelayInfoRequest>");
            return null;
        }

        DelayInfoRequest delayInfoRequest;
        try {
            delayInfoRequest = objectMapper.convertValue(crudEvent.getData(), DelayInfoRequest.class);
        } catch (IllegalArgumentException e) {
            log.error("Unexpected event parameters, expected a CrudEvent<String, DelayInfoRequest>");
            return null;
        }

        return new CrudEvent<>(crudEvent.getEventType(), (String)crudEvent.getKey(), delayInfoRequest);
    }

    private void processMessage(Message<Event<?, ?>> message) {
        log.info("Processing message created at {}...", message.getPayload().getEventCreatedAt());
        CrudEvent<String, DelayInfoRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload());
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
                            log.error("An error occurred: {}", throwable.getMessage());
                            log.warn("Sending error response message to delay data collector...");
                            ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(resolveException(throwable)), resolveHttpStatus(resolveException(throwable)));
                            responseSender.sendResponseMessage("railDataResponses-out-0", new HttpResponseEvent(HttpResponseEvent.Type.ERROR, request.getTrainNumber(), responsePayload));
                        })
                        .onErrorResume(throwable -> Mono.empty())
                        .subscribeOn(messageProcessingScheduler)
                        .subscribe();
            }
            case null, default -> handleIncorrectEventTypeError(crudEvent);
        }
    }

    private String serializeObjectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    private void handleIncorrectEventParametersError(Event<?, ?> event) {
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received Event was not an instance of CrudEvent<String, DelayInfoRequest>")), HttpStatus.BAD_REQUEST);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, event.getKey().toString(), responsePayload);
        responseSender.sendResponseMessage("railDataResponses-out-0", errorEvent);
    }

    private void handleIncorrectEventTypeError(CrudEvent<String, DelayInfoRequest> crudEvent) {
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received event had an unsupported event type")), HttpStatus.METHOD_NOT_ALLOWED);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, crudEvent.getKey(), responsePayload);
        responseSender.sendResponseMessage("railDataResponses-out-0", errorEvent);
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
