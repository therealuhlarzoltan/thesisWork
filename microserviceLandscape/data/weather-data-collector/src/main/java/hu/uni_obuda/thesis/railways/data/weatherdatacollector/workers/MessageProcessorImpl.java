package hu.uni_obuda.thesis.railways.data.weatherdatacollector.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfoRequest;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.controller.WeatherDataCollector;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfoRequest;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
public class MessageProcessorImpl implements MessageProcessor {

    private final ObjectMapper objectMapper;
    private final WeatherDataCollector weatherDataCollector;
    private final ResponseMessageSender responseSender;
    private final Scheduler messageProcessingScheduler;

    public MessageProcessorImpl(ObjectMapper objectMapper, WeatherDataCollector weatherDataCollector,
                                ResponseMessageSender responseSender, Scheduler messageProcessingScheduler) {
        this.objectMapper = objectMapper;
        this.weatherDataCollector = weatherDataCollector;
        this.responseSender = responseSender;
        this.messageProcessingScheduler = messageProcessingScheduler;
    }

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        log.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        if (eventMessage.getHeaders().containsKey("correlationId")) {
            processMessageWithCorrelationId(eventMessage);
        } else {
            processMessageWithoutCorrelationId(eventMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private CrudEvent<String, WeatherInfoRequest> retrieveCrudEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof CrudEvent<?, ?> crudEvent)) {
            log.error("Unexpected event parameters, expected a CrudEvent");
            return null;
        }
        if (!(crudEvent.getKey() instanceof String)) {
            log.error("Unexpected event parameters, expected a CrudEvent<String, WeatherInfoRequest>");
            return null;
        }

        WeatherInfoRequest weatherInfoRequest;
        try {
            weatherInfoRequest = objectMapper.convertValue(crudEvent.getData(), WeatherInfoRequest.class);
        } catch (IllegalArgumentException e) {
            log.error("Unexpected event parameters, expected a CrudEvent<String, WeatherInfoRequest>");
            return null;
        }

        return new CrudEvent<>(crudEvent.getEventType(), (String)crudEvent.getKey(), weatherInfoRequest);
    }

    private void processMessageWithoutCorrelationId(Message<Event<?, ?>> message) {
        log.info("Processing message created at {} with no correlationId...", message.getPayload().getEventCreatedAt());
        CrudEvent<String, WeatherInfoRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload(), null);
            return;
        }
        CrudEvent.Type eventType = crudEvent.getEventType();
        switch (eventType) {
            case GET -> {
                WeatherInfoRequest request = crudEvent.getData();
                Mono<WeatherInfo> weatherInfoMono = weatherDataCollector.getWeatherInfo(request.getStationName(), request.getLatitude(), request.getLongitude(), request.getTime());
                weatherInfoMono
                        .onErrorResume(throwable -> {
                            log.error("Constructed empty WeatherInfo due to error: {}", throwable.getMessage());
                            return Mono.just(WeatherInfo.builder().address(request.getStationName()).time(request.getTime()).build());
                        })
                        .switchIfEmpty(Mono.just(WeatherInfo.builder().address(request.getStationName()).time(request.getTime()).build()))
                        .flatMap(weatherInfo -> {
                            return Mono.fromCallable(() -> {
                                ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(weatherInfo), HttpStatus.OK);
                                return new HttpResponseEvent(HttpResponseEvent.Type.SUCCESS, request.getStationName(), responsePayload);
                            });
                        })
                        .doOnNext(event -> responseSender.sendResponseMessage("weatherDataResponses-out-0", event))
                        .subscribeOn(messageProcessingScheduler)
                        .subscribe();
            }
            case null, default -> handleIncorrectEventTypeError(crudEvent, null);
        }

    }

    private void processMessageWithCorrelationId(Message<Event<?, ?>> message) {
        String correlationId = message.getHeaders().get("correlationId").toString();
        log.info("Processing message created at {} with correlationId {}...", message.getPayload().getEventCreatedAt(), correlationId);
        CrudEvent<String, WeatherInfoRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload(), correlationId);
            return;
        }
        CrudEvent.Type eventType = crudEvent.getEventType();
        switch (eventType) {
            case GET -> {
                WeatherInfoRequest request = crudEvent.getData();
                Mono<WeatherInfo> weatherInfoMono = weatherDataCollector.getWeatherInfo(request.getStationName(), request.getLatitude(), request.getLongitude(), request.getTime());
                weatherInfoMono
                        .onErrorResume(throwable -> {
                            log.error("Constructed empty WeatherInfo due to error: {}", throwable.getMessage());
                            return Mono.just(WeatherInfo.builder().address(request.getStationName()).time(request.getTime()).build());
                        })
                        .switchIfEmpty(Mono.just(WeatherInfo.builder().address(request.getStationName()).time(request.getTime()).build()))
                        .flatMap(weatherInfo -> {
                            return Mono.fromCallable(() -> {
                                ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(weatherInfo), HttpStatus.OK);
                                return new HttpResponseEvent(HttpResponseEvent.Type.SUCCESS, request.getStationName(), responsePayload);
                            });
                        })
                        .doOnNext(event -> responseSender.sendResponseMessage("weatherDataResponses-out-0", correlationId, event))
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
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    private void handleIncorrectEventParametersError(Event<?, ?> event, String correlationId) {
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received Event was not an instance of CrudEvent<String, WeatherInfoRequest>")), HttpStatus.BAD_REQUEST);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, event.getKey().toString(), responsePayload);
        if (correlationId != null) {
            responseSender.sendResponseMessage("railDataResponses-out-0", correlationId, errorEvent);
        } else {
            responseSender.sendResponseMessage("railDataResponses-out-0", errorEvent);
        }
    }

    private void handleIncorrectEventTypeError(CrudEvent<String, WeatherInfoRequest> crudEvent, String correlationId) {
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
