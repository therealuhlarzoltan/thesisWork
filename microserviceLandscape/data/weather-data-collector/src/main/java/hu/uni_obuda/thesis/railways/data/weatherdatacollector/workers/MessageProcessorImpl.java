package hu.uni_obuda.thesis.railways.data.weatherdatacollector.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.controller.WeatherDataCollector;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfoRequest;
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
    private final WeatherDataCollector weatherDataCollector;
    private final ResponseMessageSender responseSender;
    private final Scheduler messageProcessingScheduler;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        log.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        processMessage(eventMessage);
    }

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

    private void processMessage(Message<Event<?, ?>> message) {
        log.info("Processing message created at {}", message.getPayload().getEventCreatedAt());
        CrudEvent<String, WeatherInfoRequest> crudEvent = retrieveCrudEvent(message.getPayload());
        if (crudEvent == null) {
            handleIncorrectEventParametersError(message.getPayload());
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
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received Event was not an instance of CrudEvent<String, WeatherInfoRequest>")), HttpStatus.BAD_REQUEST);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, event.getKey().toString(), responsePayload);
        responseSender.sendResponseMessage("railDataResponses-out-0", errorEvent);
    }

    private void handleIncorrectEventTypeError(CrudEvent<String, WeatherInfoRequest> crudEvent) {
        ResponsePayload responsePayload = new ResponsePayload(serializeObjectToJson(new MessageFormatException("The received event had an unsupported event type")), HttpStatus.METHOD_NOT_ALLOWED);
        HttpResponseEvent errorEvent = new HttpResponseEvent(HttpResponseEvent.Type.ERROR, crudEvent.getKey(), responsePayload);
        responseSender.sendResponseMessage("railDataResponses-out-0", errorEvent);
    }
}
