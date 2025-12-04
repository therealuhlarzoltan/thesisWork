package hu.uni_obuda.thesis.railways.data.geocodingservice.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.geocodingservice.controller.GeocodingController;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingRequest;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
@RequiredArgsConstructor
public class MessageProcessorImpl implements MessageProcessor {

    private final ObjectMapper objectMapper;
    private final GeocodingController geocodingController;
    private final ResponseMessageSender responseSender;
    private final Scheduler messageProcessingScheduler;


    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        log.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        processMessage(eventMessage);
    }

    private CrudEvent<String, GeocodingRequest> retrieveCrudEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof CrudEvent<?, ?> crudEvent)) {
            log.error("Unexpected event parameters, expected a CrudEvent");
            return null;
        }
        if (!(crudEvent.getKey() instanceof String)) {
            log.error("Unexpected event parameters, expected a CrudEvent<String, GeocodingRequest>");
            return null;
        }

       GeocodingRequest geocodingRequest;
        try {
            geocodingRequest = objectMapper.convertValue(crudEvent.getData(), GeocodingRequest.class);
        } catch (IllegalArgumentException e) {
            log.error("Unexpected event parameters, expected a CrudEvent<String, GeocodingRequest>");
            return null;
        }

        return new CrudEvent<>(crudEvent.getEventType(), (String)crudEvent.getKey(), geocodingRequest);
    }

    private void processMessage(Message<Event<?, ?>> message) {
        log.info("Processing message created at {}...", message.getPayload().getEventCreatedAt());
        CrudEvent<String, GeocodingRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload());
            return;
        }
        CrudEvent.Type eventType = crudEvent.getEventType();
        switch (eventType) {
            case GET -> {
                GeocodingRequest request = crudEvent.getData();
                Mono<GeocodingResponse> geocodingResponseMono = geocodingController.getCoordinates(request.getAddress());
                geocodingResponseMono
                        .flatMap(geocodingResponse -> {
                            return Mono.fromCallable(() -> {
                                ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(geocodingResponse), HttpStatus.OK);
                                return new HttpResponseEvent(HttpResponseEvent.Type.SUCCESS, request.getAddress(), responsePayload);
                            });
                        })
                        .doOnNext(event -> responseSender.sendResponseMessage("geocodingDataResponses-out-0", event))
                        .doOnError(throwable -> {
                            log.error("An error occurred: {}", throwable.getMessage());
                            log.warn("Sending error response message to delay data collector...");
                            ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(resolveException(throwable)), resolveHttpStatus(resolveException(throwable)));
                            responseSender.sendResponseMessage("geocodingDataResponses-out-0", new HttpResponseEvent(HttpResponseEvent.Type.ERROR, request.getAddress(), responsePayload));
                        })
                        .onErrorResume(_ -> Mono.empty())
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
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received Event was not an instance of CrudEvent<String, GeocodingRequest>")), HttpStatus.BAD_REQUEST);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, event.getKey().toString(), responsePayload);
        responseSender.sendResponseMessage("geocodingDataResponses-out-0", errorEvent);
    }

    private void handleIncorrectEventTypeError(CrudEvent<String, GeocodingRequest> crudEvent) {
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received event had an unsupported event type")), HttpStatus.METHOD_NOT_ALLOWED);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, crudEvent.getKey(), responsePayload);
        responseSender.sendResponseMessage("geocodingDataResponses-out-0", errorEvent);
    }

    private Throwable resolveException(Throwable throwable) {
        return switch (throwable) {
            case InvalidInputDataException invalidInputDataException -> invalidInputDataException;
            case ExternalApiException externalApiException -> externalApiException;
            case ExternalApiFormatMismatchException externalApiFormatMismatchException -> externalApiFormatMismatchException;
            case InternalApiException internalApiException -> internalApiException;
            case ApiException apiException -> apiException;
            default -> throwable;
        };
    }

    private HttpStatus resolveHttpStatus(Throwable throwable) {
        return switch (throwable) {
            case InvalidInputDataException invalidInputDataException -> HttpStatus.resolve(invalidInputDataException.getStatusCode().value());
            case ExternalApiException externalApiException -> HttpStatus.resolve(externalApiException.getStatusCode().value());
            case ExternalApiFormatMismatchException externalApiFormatMismatchException -> HttpStatus.BAD_GATEWAY;
            case InternalApiException internalApiException -> HttpStatus.INTERNAL_SERVER_ERROR;
            case ApiException apiException -> HttpStatus.BAD_GATEWAY;
            case null, default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
