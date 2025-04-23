package hu.uni_obuda.thesis.railways.data.geocodingservice.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.geocodingservice.controller.GeocodingController;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingRequest;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.controller.WeatherDataCollector;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfoRequest;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RequiredArgsConstructor
public class MessageProcessorImpl implements MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorImpl.class);

    private final ObjectMapper objectMapper;
    private final GeocodingController geocodingController;
    private final ResponseMessageSender responseSender;
    private final Scheduler messageProcessingScheduler;


    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        LOG.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        if (eventMessage.getHeaders().containsKey("correlationId")) {
            processMessageWithCorrelationId(eventMessage);
        } else {
            processMessageWithoutCorrelationId(eventMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private CrudEvent<String, GeocodingRequest> retrieveCrudEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof CrudEvent<?, ?> crudEvent)) {
            LOG.error("Unexpected event parameters, expected a CrudEvent");
            return null;
        }
        if (!(crudEvent.getKey() instanceof String)) {
            LOG.error("Unexpected event parameters, expected a CrudEvent<String, GeocodingRequest>");
            return null;
        }

       GeocodingRequest geocodingRequest;
        try {
            geocodingRequest = objectMapper.convertValue(crudEvent.getData(), GeocodingRequest.class);
        } catch (IllegalArgumentException e) {
            LOG.error("Unexpected event parameters, expected a CrudEvent<String, GeocodingRequest>");
            return null;
        }

        return new CrudEvent<>(crudEvent.getEventType(), (String)crudEvent.getKey(), geocodingRequest);
    }

    private void processMessageWithoutCorrelationId(Message<Event<?, ?>> message) {
        LOG.info("Processing message created at {} with no correlationId...", message.getPayload().getEventCreatedAt());
        CrudEvent<String, GeocodingRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload(), null);
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
                            LOG.error("An error occurred: {}", throwable.getMessage());
                            LOG.warn("Sending error response message to delay data collector...");
                            ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(resolveException(throwable)), resolveHttpStatus(resolveException(throwable)));
                            responseSender.sendResponseMessage("geocodingDataResponses-out-0", new HttpResponseEvent(HttpResponseEvent.Type.ERROR, request.getAddress(), responsePayload));
                        })
                        .onErrorResume(_ -> Mono.empty())
                        .subscribeOn(messageProcessingScheduler)
                        .subscribe();
            }
            case null, default -> handleIncorrectEventTypeError(crudEvent, null);
        }

    }

    private void processMessageWithCorrelationId(Message<Event<?, ?>> message) {
        String correlationId = message.getHeaders().get("correlationId").toString();
        LOG.info("Processing message created at {} with correlationId {}...", message.getPayload().getEventCreatedAt(), correlationId);
        CrudEvent<String, GeocodingRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload(), correlationId);
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
                        .doOnNext(event -> responseSender.sendResponseMessage("geocodingDataResponses-out-0", correlationId, event))
                        .doOnError(throwable -> {
                            LOG.error("An error occurred: {}", throwable.getMessage());
                            LOG.warn("Sending error response message to delay data collector...");
                            ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(resolveException(throwable)), resolveHttpStatus(resolveException(throwable)));
                            responseSender.sendResponseMessage("geocodingDataResponses-out-0", correlationId, new HttpResponseEvent(HttpResponseEvent.Type.ERROR, request.getAddress(), responsePayload));
                        })
                        .onErrorResume(_ -> Mono.empty())
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
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received Event was not an instance of CrudEvent<String, GeocodingRequest>")), HttpStatus.BAD_REQUEST);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, event.getKey().toString(), responsePayload);
        if (correlationId != null) {
            responseSender.sendResponseMessage("geocodingDataResponses-out-0", correlationId, errorEvent);
        } else {
            responseSender.sendResponseMessage("geocodingDataResponses-out-0", errorEvent);
        }
    }

    private void handleIncorrectEventTypeError(CrudEvent<String, GeocodingRequest> crudEvent, String correlationId) {
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received event had an unsupported event type")), HttpStatus.METHOD_NOT_ALLOWED);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, crudEvent.getKey(), responsePayload);
        if (correlationId != null) {
            responseSender.sendResponseMessage("geocodingDataResponses-out-0", correlationId, errorEvent);
        } else {
            responseSender.sendResponseMessage("geocodingDataResponses-out-0", errorEvent);
        }
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
